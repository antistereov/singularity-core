package io.stereov.singularity.user.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.encryption.exception.EncryptedDatabaseException
import io.stereov.singularity.user.core.exception.FindPrincipalByIdException
import io.stereov.singularity.user.core.model.Guest
import io.stereov.singularity.user.core.model.Principal
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.User
import io.stereov.singularity.user.core.model.encrypted.EncryptedGuest
import io.stereov.singularity.user.core.model.encrypted.EncryptedPrincipal
import io.stereov.singularity.user.core.model.encrypted.EncryptedUser
import io.stereov.singularity.user.core.model.sensitve.SensitivePrincipalData
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

/**
 * Service for handling operations related to [Principal]s in the system.
 *
 * This service provides methods to interact with [Principal] entities,
 * which can include [User]s or [Guest]s with associated roles and sensitive data.
 */
@Service
class PrincipalService(
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val userService: UserService,
    private val guestService: GuestService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Retrieves a [Principal] using its unique identifier.
     *
     * @param id The unique [ObjectId] of the principal to be retrieved.
     * @return A [Result] containing the principal of type [Principal] with its decrypted data on success,
     * or a [FindPrincipalByIdException] if an error occurs during retrieval or decryption.
     */
    suspend fun findById(id: ObjectId): Result<Principal<out Role, out SensitivePrincipalData>, FindPrincipalByIdException> = coroutineBinding {
        logger.debug { "Finding principal with id $id" }

        val encryptedPrincipal = runSuspendCatching {
            reactiveMongoTemplate.findById<EncryptedPrincipal<Role, SensitivePrincipalData>>(id, "principals")
                .awaitSingleOrNull()
        }
            .mapError { ex -> FindPrincipalByIdException.Database("Failed to retrieve principal with id $id from database", ex) }
            .andThen { it.toResultOr { FindPrincipalByIdException.NotFound("No principal found with id $id") }}
            .bind()

        val decryptedPrincipal = when (encryptedPrincipal) {
            is EncryptedUser -> userService.decrypt(encryptedPrincipal)
            is EncryptedGuest -> guestService.decrypt(encryptedPrincipal)
        }

        decryptedPrincipal.mapError { ex -> FindPrincipalByIdException.Encryption("Failed to decrypt principal with id $id: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Checks if a [Principal] with the given ID exists in the database.
     *
     * @param id The unique ObjectId of the principal to check for existence.
     * @return A [Result] containing a [Boolean] indicating whether the principal exists
     *   or a [FindPrincipalByIdException] if an error occurs during the check.
     */
    @Suppress("UNUSED")
    suspend fun existsById(id: ObjectId): Result<Boolean, FindPrincipalByIdException> = coroutineBinding {
        logger.debug { "Checking existence of principal with id $id" }

        val query = Query.query(Criteria.where("_id").`is`(id))

        runSuspendCatching {
            reactiveMongoTemplate.exists<EncryptedPrincipal<Role, SensitivePrincipalData>>(query, "principals")
                .awaitSingleOrNull()
        }
            .mapError { ex -> FindPrincipalByIdException.Database("Failed to check existence of principal with id $id from database", ex) }
            .andThen { it.toResultOr { FindPrincipalByIdException.NotFound("No principal found with id $id") }}
            .bind()
    }

    /**
     * Saves the provided [Principal] into the appropriate service based on its type.
     *
     * @param document The [Principal] object containing [Role] and [SensitivePrincipalData]
     *                 to be saved. It can either be a [User] or a [Guest].
     * @return A [Result] containing the saved [Principal]
     * on success, or an [EncryptedDatabaseException] if an error occurs during saving.
     */
    suspend fun save(
        document: Principal<out Role, out SensitivePrincipalData>
    ): Result<Principal<out Role, out SensitivePrincipalData>, EncryptedDatabaseException> {
        return when (document) {
            is User -> userService.save(document)
            is Guest -> guestService.save(document)
        }
    }
}
