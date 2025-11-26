package io.stereov.singularity.user.core.service

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.encryption.exception.EncryptedDatabaseException
import io.stereov.singularity.database.encryption.exception.EncryptionException
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.encryption.service.SensitiveCrudService
import io.stereov.singularity.user.core.dto.request.CreateGuestRequest
import io.stereov.singularity.user.core.mapper.GuestMapper
import io.stereov.singularity.user.core.model.Guest
import io.stereov.singularity.user.core.model.encrypted.EncryptedGuest
import io.stereov.singularity.user.core.model.sensitve.SensitiveGuestData
import io.stereov.singularity.user.core.repository.GuestRepository
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
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
     * @return a [Result] containing the created [Guest] on success, or an [EncryptedDatabaseException] in case of an error
     */
    suspend fun createGuest(req: CreateGuestRequest): Result<Guest, EncryptedDatabaseException> {
        logger.debug { "Creating guest with name ${req.name}" }

        val user = guestMapper.createGuest(
            name = req.name,
        )

        return save(user)
    }
}
