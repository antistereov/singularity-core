package io.stereov.singularity.core.user.service.twofactor

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.auth.exception.AuthException
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.auth.service.CookieService
import io.stereov.singularity.core.global.service.cache.AccessTokenCache
import io.stereov.singularity.core.global.service.hash.HashService
import io.stereov.singularity.core.global.service.random.RandomService
import io.stereov.singularity.core.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.singularity.core.properties.TwoFactorAuthProperties
import io.stereov.singularity.core.user.dto.UserDto
import io.stereov.singularity.core.user.dto.request.DisableTwoFactorRequest
import io.stereov.singularity.core.user.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.core.user.service.UserService
import io.stereov.singularity.core.user.service.token.TwoFactorAuthTokenService
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
    private val authenticationService: AuthenticationService,
    private val twoFactorAuthProperties: TwoFactorAuthProperties,
    private val hashService: HashService,
    private val cookieService: CookieService,
    private val twoFactorAuthTokenService: TwoFactorAuthTokenService,
    private val accessTokenCache: AccessTokenCache,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Sets up two-factor authentication for the current user.
     * It generates a secret key, an OTP auth URL, a recovery code, and a token.
     * The token is used to validate the setup process and enable two-factor authentication for the current user.
     *
     * It needs a valid step-up token to perform this action.
     *
     * @return A [TwoFactorSetupResponse] containing the secret, OTP auth URL, recovery code and setup token.
     */
    suspend fun setUpTwoFactorAuth(exchange: ServerWebExchange): TwoFactorSetupResponse {
        logger.debug { "Setting up two factor authentication" }

        cookieService.validateTwoFactorSetupCookie(exchange)

        val user = authenticationService.getCurrentUser()

        val secret = twoFactorAuthService.generateSecretKey()
        val otpAuthUrl = twoFactorAuthService.getOtpAuthUrl(user.sensitive.email, secret)
        val recoveryCodes = List(twoFactorAuthProperties.recoveryCodeCount) {
            RandomService.generateCode(twoFactorAuthProperties.recoveryCodeLength)
        }

        val setupToken = twoFactorAuthTokenService.createSetupToken(user.id, secret, recoveryCodes)

        return TwoFactorSetupResponse(secret, otpAuthUrl, recoveryCodes, setupToken)
    }

    /**
     * Validates the setup token and enables two-factor authentication for the current user.
     *
     * @param token The setup token to validate.
     * @param code The two-factor authentication code to validate.
     *
     * @throws InvalidDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws AuthException If the setup token is invalid.
     *
     * @return The updated user document.
     */
    suspend fun validateSetup(token: String, code: Int): UserDto {
        val user = authenticationService.getCurrentUser()
        val setupToken = twoFactorAuthTokenService.validateAndExtractSetupToken(token)

        if (!twoFactorAuthService.validateCode(setupToken.secret, code)) {
            throw AuthException("Invalid two-factor authentication code")
        }

        val encryptedSecret = setupToken.secret
        val hashedRecoveryCodes = setupToken.recoveryCodes.map {
            hashService.hashBcrypt(it)
        }

        user.setupTwoFactorAuth(encryptedSecret, hashedRecoveryCodes)
            .clearDevices()

        userService.save(user)
        accessTokenCache.invalidateAllTokens(user.id)

        return user.toDto()
    }

    /**
     * Validates the two-factor code for the current user.
     *
     * @param exchange The server web exchange containing the request and response.
     * @param code The two-factor code to validate.
     *
     * @throws InvalidDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws AuthException If the two-factor code is invalid.
     */
    suspend fun validateTwoFactorCode(exchange: ServerWebExchange, code: Int): UserDocument {
        logger.debug { "Validating two factor code" }

        val userId = cookieService.validateLoginVerificationCookieAndGetUserId(exchange)

        val user = userService.findById(userId)

        return twoFactorAuthService.validateTwoFactorCode(user, code)
    }

    /**
     * Checks if the user has two-factor authentication pending.
     *
     * @param exchange The server web exchange containing the request and response.
     *
     * @return True if two-factor authentication is pending, false otherwise.
     */
    suspend fun loginVerificationNeeded(exchange: ServerWebExchange): Boolean {
        logger.debug { "Checking login verification is needed" }

        return try {
            cookieService.validateLoginVerificationCookieAndGetUserId(exchange)
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
     * @throws AuthException If the recovery code is invalid.
     *
     * @return The user document after recovery.
     */
    suspend fun recoverUser(exchange: ServerWebExchange, recoveryCode: String): UserDocument {
        logger.debug { "Recovering user and clearing all devices" }

        val userId = try {
            authenticationService.getCurrentUserId()
        } catch (e: Exception) {
            cookieService.validateLoginVerificationCookieAndGetUserId(exchange)
        }

        val user = userService.findById(userId)
        val recoveryCodeHashes = user.sensitive.security.twoFactor.recoveryCodes

        val match = recoveryCodeHashes.removeAll { hash ->
            hashService.checkBcrypt(recoveryCode, hash)
        }

        if (!match) {
            throw AuthException("Invalid recovery code")
        }

        return userService.save(user)
    }

    /**
     * Disables two-factor authentication for the current user.
     *
     * @return The updated user document.
     */
    suspend fun disable(exchange: ServerWebExchange, req: DisableTwoFactorRequest): UserDto {
        logger.debug { "Disabling 2FA" }

        cookieService.validateStepUpCookie(exchange)

        val user = authenticationService.getCurrentUser()

        if (!hashService.checkBcrypt(req.password, user.password)) {
            throw AuthException("Password is wrong")
        }

        user.disableTwoFactorAuth()

        return userService.save(user).toDto()
    }
}
