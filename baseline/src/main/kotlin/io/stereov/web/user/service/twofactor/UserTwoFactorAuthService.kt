package io.stereov.web.user.service.twofactor

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.auth.service.CookieService
import io.stereov.web.global.service.encryption.EncryptionService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.web.properties.TwoFactorAuthProperties
import io.stereov.web.user.dto.response.TwoFactorSetupResponse
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.exception.model.InvalidUserDocumentException
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

/**
 * # Service for managing two-factor authentication (2FA) for users.
 *
 * This service provides methods to set up, validate, and recover two-factor authentication for users.
 * It uses the [TwoFactorAuthService] to generate and validate codes,
 * the [EncryptionService] to encrypt and decrypt secrets,
 * and the [HashService] to hash and check recovery codes.
 * It interacts with the [UserService] to save user data and the [AuthenticationService] to get the current user.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class UserTwoFactorAuthService(
    private val userService: UserService,
    private val twoFactorAuthService: TwoFactorAuthService,
    private val encryptionService: EncryptionService,
    private val authenticationService: AuthenticationService,
    private val twoFactorAuthProperties: TwoFactorAuthProperties,
    private val hashService: HashService,
    private val cookieService: CookieService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Sets up two-factor authentication for the current user.
     *
     * @throws AuthException If the user document does not contain an ID.
     *
     * @return A [TwoFactorSetupResponse] containing the secret, OTP auth URL, and recovery code.
     */
    suspend fun setUpTwoFactorAuth(): TwoFactorSetupResponse {
        logger.debug { "Setting up two factor authentication" }

        val user = authenticationService.getCurrentUser()

        val secret = twoFactorAuthService.generateSecretKey()
        val otpAuthUrl = twoFactorAuthService.getOtpAuthUrl(user.email, secret)
        val recoveryCode = twoFactorAuthService.generateRecoveryCode(twoFactorAuthProperties.recoveryCodeLength)

        val encryptedSecret = encryptionService.encrypt(secret)
        val hashedRecoveryCode = hashService.hashBcrypt(recoveryCode)

        userService.save(user.setupTwoFactorAuth(encryptedSecret, hashedRecoveryCode))

        return TwoFactorSetupResponse(secret, otpAuthUrl, recoveryCode)
    }

    /**
     * Validates the two-factor code for the current user.
     *
     * @param exchange The server web exchange containing the request and response.
     * @param code The two-factor code to validate.
     *
     * @throws InvalidUserDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws AuthException If the two-factor code is invalid.
     */
    suspend fun validateTwoFactorCode(exchange: ServerWebExchange, code: Int): UserDto {
        logger.debug { "Validating two factor code" }

        val userId = cookieService.validateTwoFactorSessionCookieAndGetUserId(exchange)

        val user = userService.findById(userId)

        return validateTwoFactorCode(user, code).toDto()
    }

    /**
     * Validates the two-factor code for the given user. It throws an exception if the code is invalid.
     *
     * @param user The user to validate the code for.
     * @param code The two-factor code to validate.
     *
     * @throws InvalidUserDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws AuthException If the two-factor code is invalid.
     */
    suspend fun validateTwoFactorCode(user: UserDocument, code: Int): UserDocument {
        val encryptedSecret = user.security.twoFactor.secret
            ?: throw InvalidUserDocumentException("No two factor authentication secret provided in UserDocument")
        val decryptedSecret = encryptionService.decrypt(encryptedSecret)

        if (!twoFactorAuthService.validateCode(decryptedSecret, code)) {
            throw AuthException("Invalid 2FA code")
        }

        return user
    }

    /**
     * Checks if the user has two-factor authentication pending.
     *
     * @param exchange The server web exchange containing the request and response.
     *
     * @return True if two-factor authentication is pending, false otherwise.
     */
    suspend fun twoFactorPending(exchange: ServerWebExchange): Boolean {
        logger.debug { "Checking two factor authentication status" }

        return try {
            cookieService.validateTwoFactorSessionCookieAndGetUserId(exchange)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Recovers the user by validating the recovery code and clearing all devices and
     * therefore, signing out the user on all devices.
     *
     * @param exchange The server web exchange containing the request and response.
     * @param recoveryCode The recovery code to validate.
     *
     * @throws InvalidUserDocumentException If the user document does not contain a recovery code.
     * @throws AuthException If the recovery code is invalid.
     *
     * @return The user document after recovery.
     */
    suspend fun recoverUser(exchange: ServerWebExchange, recoveryCode: String): UserDto {
        logger.debug { "Recovering user and clearing all devices" }

        val userId = cookieService.validateTwoFactorSessionCookieAndGetUserId(exchange)

        val user = userService.findById(userId)
        val recoveryCodeHash = user.security.twoFactor.recoveryCode
            ?: throw InvalidUserDocumentException("No recovery code saved in UserDocument")

        if (!hashService.checkBcrypt(recoveryCode, recoveryCodeHash)) {
            throw AuthException("Invalid recovery code")
        }

        user.disableTwoFactorAuth()
        user.clearDevices()

        return userService.save(user).toDto()
    }
}
