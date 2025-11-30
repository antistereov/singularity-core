package io.stereov.singularity.principal.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.encryption.exception.EncryptionException
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.encryption.service.SensitiveCrudService
import io.stereov.singularity.principal.core.dto.request.CreateGuestRequest
import io.stereov.singularity.principal.core.mapper.GuestMapper
import io.stereov.singularity.principal.core.model.Guest
import io.stereov.singularity.principal.core.model.encrypted.EncryptedGuest
import io.stereov.singularity.principal.core.model.encrypted.EncryptedPrincipal
import io.stereov.singularity.principal.core.model.encrypted.EncryptedUser
import io.stereov.singularity.principal.core.model.sensitve.SensitiveGuestData
import io.stereov.singularity.principal.core.repository.GuestRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service

/**
 * Service class responsible for managing Guest domain operations.
 * Extends [SensitiveCrudService] to handle sensitive data encryption and decryption.
 *
 * This service provides functionality to create, encrypt, decrypt, and persist guest data
 * using secure mechanisms. It integrates with a reactive MongoDB database and an encryption
 * service to ensure sensitive data is safely handled.
 *
 * @property repository the data repository for guest entities
 * @property encryptionService the service managing sensitive data encryption
 * @property reactiveMongoTemplate the reactive template for MongoDB operations
 * @property encryptionSecretService the service for managing encryption secrets
 * @property guestMapper responsible for mapping guest data to entities
 */
@Service
class GuestService (
    override val repository: GuestRepository,
    override val encryptionService: EncryptionService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    override val encryptionSecretService: EncryptionSecretService,
    private val guestMapper: GuestMapper,
) : SensitiveCrudService<SensitiveGuestData, Guest, EncryptedGuest>() {

    override val sensitiveClazz = SensitiveGuestData::class.java
    override val encryptedDocumentClazz= EncryptedGuest::class.java
    override val logger = KotlinLogging.logger {}

    override suspend fun doEncrypt(
        document: Guest,
        encryptedSensitive: Encrypted<SensitiveGuestData>
    ): Result<EncryptedGuest, EncryptionException> {
        return Ok(
            EncryptedGuest(
            document._id,
            document.createdAt,
            document.lastActive,
            encryptedSensitive
        ))
    }

    override suspend fun doDecrypt(
        encrypted: EncryptedGuest,
        decryptedSensitive: SensitiveGuestData,
    ): Result<Guest, EncryptionException> {
        return Ok(
        Guest(
            encrypted._id,
            encrypted.createdAt,
            encrypted.lastActive,
            decryptedSensitive
        ))
    }

    /**
     * Creates a new guest based on the provided request details.
     *
     * @param req the request object containing the guest's name and session information
     * @return a [Result] containing the created [Guest] on success, or an [SaveEncryptedDocumentException] in case of an error
     */
    suspend fun createGuest(req: CreateGuestRequest): Result<Guest, SaveEncryptedDocumentException> {
        logger.debug { "Creating guest with name ${req.name}" }

        val guest = guestMapper.createGuest(
            name = req.name,
        )

        return save(guest)
    }

    override suspend fun findById(id: ObjectId): Result<Guest, FindEncryptedDocumentByIdException> = coroutineBinding {
        val encryptedPrincipal = runSuspendCatching {
            reactiveMongoTemplate.findById<EncryptedPrincipal<*, *>>(id, "principals")
                .awaitSingleOrNull()
        }
            .mapError { ex -> FindEncryptedDocumentByIdException.Database("Failed to retrieve guest with id $id from database", ex) }
            .andThen { it.toResultOr { FindEncryptedDocumentByIdException.NotFound("No guest found with id $id") }}
            .bind()

        val decryptedPrincipal = when (encryptedPrincipal) {
            is EncryptedGuest -> decrypt(encryptedPrincipal)
            is EncryptedUser -> Err(FindEncryptedDocumentByIdException.NotFound("No guest found with id $id")).bind()
        }

        decryptedPrincipal.mapError { ex -> FindEncryptedDocumentByIdException.Encryption("Failed to decrypt guest with id $id: ${ex.message}", ex) }
            .bind()
    }
}
