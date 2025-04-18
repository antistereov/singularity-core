package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.database.service.SensitiveCrudService
import io.stereov.web.global.service.encryption.component.EncryptedTransformer
import io.stereov.web.global.service.file.exception.model.NoSuchFileException
import io.stereov.web.global.service.file.model.FileMetaData
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.user.exception.model.UserDoesNotExistException
import io.stereov.web.user.model.EncryptedUserDocument
import io.stereov.web.user.model.SensitiveUserData
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
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
    private val transformer: EncryptedTransformer,
    json: Json,
    private val hashService: HashService,
    keyManager: KeyManager,
) : SensitiveCrudService<SensitiveUserData, UserDocument, EncryptedUserDocument>(userRepository, keyManager) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override val serializer = json.serializersModule.serializer<SensitiveUserData>()

    override suspend fun encrypt(document: UserDocument, otherValues: List<Any>): EncryptedUserDocument {

        if (otherValues.getOrNull(0) == true || otherValues.getOrNull(0) == null) document.updateLastActive()

        val hashedEmail = hashService.hashSha256(document.sensitive.email)
        return this.transformer.encrypt(document, this.serializer, listOf(hashedEmail)) as EncryptedUserDocument
    }

    override suspend fun decrypt(encrypted: EncryptedUserDocument, otherValues: List<Any>): UserDocument {
        return this.transformer.decrypt(encrypted, this.serializer) as UserDocument
    }

    override suspend fun rotateKey(): Flow<EncryptedUserDocument> {
        return this.userRepository.findAll().map {
            this.userRepository.save(this.encrypt(this.decrypt(it), listOf(false)))
        }
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

    suspend fun getAvatar(userId: String): FileMetaData {
        logger.debug { "Finding avatar for user $userId" }

        val user = findById(userId)
        return user.sensitive.avatar ?: throw NoSuchFileException("No avatar set for user")
    }
}
