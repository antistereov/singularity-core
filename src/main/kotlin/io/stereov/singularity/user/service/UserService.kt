package io.stereov.singularity.user.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.service.SensitiveCrudService
import io.stereov.singularity.encryption.service.EncryptionSecretService
import io.stereov.singularity.encryption.service.EncryptionService
import io.stereov.singularity.file.core.exception.model.NoSuchFileException
import io.stereov.singularity.file.core.model.FileMetaData
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.user.exception.model.UserDoesNotExistException
import io.stereov.singularity.user.model.EncryptedUserDocument
import io.stereov.singularity.user.model.SensitiveUserData
import io.stereov.singularity.user.model.UserDocument
import io.stereov.singularity.user.repository.UserRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

/**
 * # Service for managing user accounts.
 *
 * This service provides methods to find, save, and delete user accounts.
 * It interacts with the [UserRepository] to perform database operations.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class UserService(
    override val repository: UserRepository,
    override val encryptionService: EncryptionService,
    private val hashService: HashService,
    override val encryptionSecretService: EncryptionSecretService,
) : SensitiveCrudService<SensitiveUserData, UserDocument, EncryptedUserDocument> {

    override val logger = KotlinLogging.logger {}
    override val clazz = SensitiveUserData::class.java

    override suspend fun encrypt(document: UserDocument, otherValues: List<Any>): EncryptedUserDocument {

        if (otherValues.getOrNull(0) == true || otherValues.getOrNull(0) == null) document.updateLastActive()

        val hashedEmail = hashService.hashSearchableHmacSha256(document.sensitive.email)
        return this.encryptionService.encrypt(document, listOf(hashedEmail)) as EncryptedUserDocument
    }

    override suspend fun rotateSecret() {
        logger.debug { "Rotating encryption secrets for users" }

        this.repository.findAll()
            .map {
                if (it.sensitive.secretKey == encryptionSecretService.getCurrentSecret().key) {
                    logger.debug { "Skipping rotation of user document ${it._id}: Encryption secret did not change" }
                    return@map it
                }

                this.logger.debug { "Rotating key of user document ${it._id}" }
                this.repository.save(this.encrypt(this.decrypt(it), listOf(false)))
            }
            .onCompletion { logger.debug { "Key successfully rotated" } }
            .collect {}
    }

    /**
     * Finds a user by their email address.
     *
     * @param email The email address of the user to find.
     *
     * @return The [UserDocument] of the found user.
     *
     * @throws UserDoesNotExistException If no user is found with the given email.
     */
    suspend fun findByEmail(email: String): UserDocument {
        logger.debug { "Fetching user with email $email" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        val encrypted =  this.repository.findByEmail(hashedEmail)
            ?: throw UserDoesNotExistException("No user account found with email $email")

        return this.decrypt(encrypted)
    }

    /**
     * Finds a user by their email address, returning null if not found.
     *
     * @param email The email address of the user to find.
     *
     * @return The [UserDocument] of the found user, or null if not found.
     */
    suspend fun findByEmailOrNull(email: String): UserDocument? {
        logger.debug { "Fetching user with email $email" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        return this.repository.findByEmail(hashedEmail)
            ?.let { this. decrypt(it) }
    }

    /**
     * Checks if a user exists by their email address.
     *
     * @param email The email address to check.
     *
     * @return True if a user with the given email exists, false otherwise.
     */
    suspend fun existsByEmail(email: String): Boolean {
        logger.debug { "Checking if email $email already exists" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        return this.repository.existsByEmail(hashedEmail)
    }

    suspend fun getAvatar(userId: ObjectId): FileMetaData {
        logger.debug { "Finding avatar for user $userId" }

        val user = findById(userId)
        return user.sensitive.avatar ?: throw NoSuchFileException("No avatar set for user")
    }
}
