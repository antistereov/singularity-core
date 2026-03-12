package io.stereov.singularity.principal.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.database.encryption.exception.*
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.principal.core.exception.FindPrincipalByIdException
import io.stereov.singularity.principal.core.model.Guest
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.encrypted.EncryptedGuest
import io.stereov.singularity.principal.core.model.encrypted.EncryptedPrincipal
import io.stereov.singularity.principal.core.model.encrypted.EncryptedUser
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.findAll
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
    private val guestService: GuestService,
    private val fileStorage: FileStorage
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
     *   or a [ExistsEncryptedDocumentByIdException] if an error occurs during the check.
     */
    @Suppress("UNUSED")
    suspend fun existsById(id: ObjectId): Result<Boolean, ExistsEncryptedDocumentByIdException> = coroutineBinding {
        logger.debug { "Checking existence of principal with id $id" }

        val query = Query.query(Criteria.where("_id").`is`(id))

        runSuspendCatching {
            reactiveMongoTemplate.exists<EncryptedPrincipal<Role, SensitivePrincipalData>>(query, "principals")
                .awaitSingleOrNull()
        }
            .mapError { ex -> ExistsEncryptedDocumentByIdException.Database("Failed to check existence of principal with id $id from database", ex) }
            .andThen { it.toResultOr { ExistsEncryptedDocumentByIdException.Database("No principal found with id $id") }}
            .bind()
    }

    /**
     * Saves the provided [Principal] into the appropriate service based on its type.
     *
     * @param document The [Principal] object containing [Role] and [SensitivePrincipalData]
     *                 to be saved. It can either be a [User] or a [Guest].
     * @return A [Result] containing the saved [Principal]
     * on success, or an [SaveEncryptedDocumentException] if an error occurs during saving.
     */
    suspend fun save(
        document: Principal<out Role, out SensitivePrincipalData>
    ): Result<Principal<out Role, out SensitivePrincipalData>, SaveEncryptedDocumentException> {
        return when (document) {
            is User -> userService.save(document)
            is Guest -> guestService.save(document)
        }
    }

    /**
     * Deletes a principal document from the database by its unique ID.
     *
     * @param id The unique [ObjectId] of the principal document to be deleted.
     * @return A [Result] containing [Unit] on successful deletion, or a [DeleteEncryptedDocumentByIdException]
     * if an error occurs during the deletion process.
     */
    suspend fun deleteById(id: ObjectId): Result<Unit, DeleteEncryptedDocumentByIdException> = coroutineBinding {
        val principal = findById(id)
            .mapError { ex -> when (ex) {
                is FindPrincipalByIdException.NotFound -> DeleteEncryptedDocumentByIdException.NotFound("No principal with ID $id found: ${ex.message}", ex)
                else -> DeleteEncryptedDocumentByIdException.Database("Failed to retrieve principal with ID $id: ${ex.message}", ex)
            } }
            .bind()

        runSuspendCatching {
            reactiveMongoTemplate.remove(Query(Criteria.where(Principal<*,*>::_id.name).`is`(id)), Principal::class.java,"principals")
                .awaitSingle()
        }
            .onSuccess {
                if (principal is User) {
                    principal.sensitive.avatarFileKey?.let { fileStorage.remove(it)
                        .onFailure { ex -> logger.error(ex) { "Failed to remove avatar of deleted user with ID $id: ${ex.message}" } }}
                }
            }
            .mapError { ex -> DeleteEncryptedDocumentByIdException.Database("Failed to delete principal with id ${id}: ${ex.message}", ex) }

            .map { }
    }

    /**
     * Retrieves a flow of all [Principal] entities, including both [User]s and [Guest]s.
     * The retrieved principals are decrypted and returned as individual results within the flow.
     *
     * @return A [Result] containing a [Flow] of [Result] where each result represents a
     * [Principal] with decrypted data or an [EncryptionException] if decryption fails.
     * If an error occurs during the retrieval process from the database, a [DatabaseException.Database] is returned.
     */
    suspend fun findAll(): Result<Flow<Result<Principal<out Role, out SensitivePrincipalData>, EncryptionException>>, DatabaseException.Database> {

        return runSuspendCatching {
            reactiveMongoTemplate.findAll<EncryptedPrincipal<out Role, out SensitivePrincipalData>>("principals")
        }
            .mapError { ex -> DatabaseException.Database("Failed to get all principals: ${ex.message}", ex) }
            .map { flux -> flux.asFlow().map { principal ->
                when (principal) {
                    is EncryptedGuest -> guestService.decrypt(principal)
                    is EncryptedUser -> userService.decrypt(principal)
                }
            } }
    }

    suspend fun deleteAll(): Result<Unit, DeleteAllEncryptedDocumentsException> {
        return userService.deleteAll()
            .andThen({ guestService.deleteAll() })
    }
}
