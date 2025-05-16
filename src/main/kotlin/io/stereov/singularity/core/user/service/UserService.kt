package io.stereov.singularity.core.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.database.service.SensitiveCrudService
import io.stereov.singularity.core.global.service.encryption.service.EncryptionService
import io.stereov.singularity.core.global.service.file.exception.model.NoSuchFileException
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.core.global.service.hash.HashService
import io.stereov.singularity.core.global.service.secrets.service.EncryptionSecretService
import io.stereov.singularity.core.user.exception.model.UserDoesNotExistException
import io.stereov.singularity.core.user.model.EncryptedUserDocument
import io.stereov.singularity.core.user.model.SensitiveUserData
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.core.user.repository.UserRepository
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
    private val userRepository: UserRepository,
    private val encryptionService: EncryptionService,
    private val hashService: HashService,
    private val encryptionSecretService: EncryptionSecretService,
) : SensitiveCrudService<SensitiveUserData, UserDocument, EncryptedUserDocument>(userRepository, encryptionSecretService, encryptionService) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override val clazz = SensitiveUserData::class.java

    override suspend fun encrypt(document: UserDocument, otherValues: List<Any>): EncryptedUserDocument {

        if (otherValues.getOrNull(0) == true || otherValues.getOrNull(0) == null) document.updateLastActive()

        val hashedEmail = hashService.hashSha256(document.sensitive.email)
        return this.encryptionService.encrypt(document, listOf(hashedEmail)) as EncryptedUserDocument
    }

    override suspend fun rotateKey() {
        logger.debug { "Rotating encryption secrets for users" }

        this.userRepository.findAll()
            .map {
                if (it.sensitive.secretId == encryptionSecretService.getCurrentSecret().id) {
                    logger.debug { "Skipping rotation of user document ${it._id}: Encryption secret did not change" }
                    return@map it
                }

                this.logger.debug { "Rotating key of user document ${it._id}" }
                this.userRepository.save(this.encrypt(this.decrypt(it), listOf(false)))
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

        val hashedEmail = hashService.hashSha256(email)
        val encrypted =  this.userRepository.findByEmail(hashedEmail)
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

        val hashedEmail = hashService.hashSha256(email)
        return this.userRepository.findByEmail(hashedEmail)
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

        val hashedEmail = hashService.hashSha256(email)
        return this.userRepository.existsByEmail(hashedEmail)
    }

    suspend fun getAvatar(userId: ObjectId): FileMetaData {
        logger.debug { "Finding avatar for user $userId" }

        val user = findById(userId)
        return user.sensitive.avatar ?: throw NoSuchFileException("No avatar set for user")
    }
}
