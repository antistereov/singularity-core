package io.stereov.singularity.auth.session.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.model.InvalidCredentialsException
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.device.service.UserDeviceService
import io.stereov.singularity.auth.session.dto.request.LoginRequest
import io.stereov.singularity.auth.session.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.session.cache.AccessTokenCache
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.mail.user.service.UserMailSender
import io.stereov.singularity.user.core.exception.model.EmailAlreadyExistsException
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service

/**
 * # Service for managing user sessions and authentication.
 *
 * This service provides methods for user login, registration, email and password changes,
 * application info retrieval, and user logout.
 * It interacts with the [UserService] to manage user data,
 * the [HashService] for password hashing,
 * the [AuthenticationService] for authentication-related operations,
 * the [UserDeviceService] for managing user devices,
 * and the [io.stereov.singularity.auth.twofactor.service.UserTwoFactorAuthService] for two-factor authentication.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class UserSessionService(
    private val userService: UserService,
    private val hashService: HashService,
    private val authenticationService: AuthenticationService,
    private val deviceService: UserDeviceService,
    private val accessTokenCache: AccessTokenCache,
    private val mailService: UserMailSender,
    private val appProperties: AppProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Logs in a user and returns the user document.
     *
     * @param payload The login request containing the user's email and password.
     *
     * @return The [UserDocument] of the logged-in user.
     *
     * @throws InvalidCredentialsException If the email or password is invalid.
     * @throws io.stereov.singularity.auth.core.exception.AuthException If the user document does not contain an ID.
     */
    suspend fun checkCredentialsAndGetUser(payload: LoginRequest): UserDocument {
        logger.debug { "Logging in user ${payload.email}" }

        val user = userService.findByEmailOrNull(payload.email)
            ?: throw InvalidCredentialsException()

        if (!hashService.checkBcrypt(payload.password, user.password)) {
            throw InvalidCredentialsException()
        }

        return userService.save(user)
    }

    /**
     * Registers a new user and returns the user document.
     *
     * @param payload The registration request containing the user's email, password, and name.
     *
     * @return The [UserDocument] of the registered user.
     *
     * @throws EmailAlreadyExistsException If the email already exists in the system.
     * @throws io.stereov.singularity.auth.core.exception.AuthException If the user document does not contain an ID.
     */
    suspend fun registerAndGetUser(payload: RegisterUserRequest, sendEmail: Boolean, lang: Language): UserDocument {
        logger.debug { "Registering user ${payload.email}" }

        if (userService.existsByEmail(payload.email)) {
            throw EmailAlreadyExistsException("Failed to register user ${payload.email}")
        }

        val userDocument = UserDocument(
            email = payload.email,
            password = hashService.hashBcrypt(payload.password),
            name = payload.name,
        )

        val savedUserDocument = userService.save(userDocument)

        if (sendEmail && appProperties.enableMail) mailService.sendVerificationEmail(savedUserDocument, lang)

        return savedUserDocument
    }

    /**
     * Logs out the user from the specified device and returns the updated user document.
     *
     * @param deviceId The ID of the device to log out from.
     *
     * @return The [UserDocument] of the logged-out user.
     */
    suspend fun logout(deviceId: String): UserDocument {
        logger.debug { "Logging out user" }

        val userId = authenticationService.getCurrentUserId()
        val tokenId = authenticationService.getCurrentTokenId()

        accessTokenCache.removeTokenId(userId, tokenId)

        return deviceService.removeDevice(deviceId)
    }

    /**
     * Logs out the user from all devices and returns the updated user document.
     *
     * @return The [UserDocument] of the logged-out user.
     */
    suspend fun logoutAllDevices(): UserDocument {
        logger.debug { "Logging out all devices" }

        val userId = authenticationService.getCurrentUserId()
        accessTokenCache.invalidateAllTokens(userId)
        val user = deviceService.clearDevices()

        return user
    }
}
