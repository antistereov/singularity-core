package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.twofactor.dto.request.DisableTwoFactorRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class UserTwoFactorAuthService(
    private val userService: UserService,
    private val twoFactorAuthService: TwoFactorAuthService,
    private val authorizationService: AuthorizationService,
    private val twoFactorAuthProperties: TwoFactorAuthProperties,
    private val hashService: HashService,
    private val accessTokenCache: AccessTokenCache,
    private val userMapper: UserMapper,
    private val stepUpTokenService: StepUpTokenService,
    private val initTokenService: TwoFactorInitSetupTokenService,
    private val setupTokenService: TwoFactorSetupTokenService,
    private val loginTokenService: TwoFactorLoginTokenService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Sets up two-factor authentication for the current user.
     * It generates a secret key, an OTP auth URL, a recovery code, and a token.
     * The token is used to validate the setup process and enable two-factor authentication for the current user.
     *
     * It needs a valid two factor setup init token to perform this action.
     *
     * @return A [TwoFactorSetupResponse] containing the secret, OTP auth URL, recovery code and setup token.
     */
    suspend fun setUpTwoFactorAuth(exchange: ServerWebExchange): TwoFactorSetupResponse {
        logger.debug { "Setting up two factor authentication" }

        initTokenService.extract(exchange)

        val user = authorizationService.getCurrentUser()

        val secret = twoFactorAuthService.generateSecretKey()
        val otpAuthUrl = twoFactorAuthService.getOtpAuthUrl(user.sensitive.email, secret)
        val recoveryCodes = List(twoFactorAuthProperties.recoveryCodeCount) {
            Random.generateCode(twoFactorAuthProperties.recoveryCodeLength)
        }

        val setupToken = setupTokenService.create(user.id, secret, recoveryCodes)

        return TwoFactorSetupResponse(secret, otpAuthUrl, recoveryCodes, setupToken.value)
    }

    /**
     * Validates the setup token and enables two-factor authentication for the current user.
     *
     * @param token The setup token to validate.
     * @param code The two-factor authentication code to validate.
     *
     * @throws io.stereov.singularity.global.exception.model.InvalidDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws io.stereov.singularity.auth.core.exception.AuthException If the setup token is invalid.
     *
     * @return The updated user document.
     */
    suspend fun validateSetup(token: String, code: Int): UserResponse {
        val user = authorizationService.getCurrentUser()
        val setupToken = setupTokenService.validate(token)

        if (!twoFactorAuthService.validateCode(setupToken.secret, code)) {
            throw AuthException("Invalid two-factor authentication code")
        }

        val encryptedSecret = setupToken.secret
        val hashedRecoveryCodes = setupToken.recoveryCodes.map {
            hashService.hashBcrypt(it)
        }

        user.setupTwoFactorAuth(encryptedSecret, hashedRecoveryCodes)
            .clearsessions()

        userService.save(user)
        accessTokenCache.invalidateAllTokens(user.id)

        return userMapper.toResponse(user)
    }

    /**
     * Validates the two-factor code for the current user.
     *
     * @param exchange The server web exchange containing the request and response.
     * @param code The two-factor code to validate.
     *
     * @throws io.stereov.singularity.global.exception.model.InvalidDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws AuthException If the two-factor code is invalid.
     */
    suspend fun validateTwoFactorCode(exchange: ServerWebExchange, code: Int): UserDocument {
        logger.debug { "Validating two factor code" }

        val token = loginTokenService.extract(exchange)

        val user = userService.findById(token.userId)

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
            loginTokenService.extract(exchange)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Recovers the user by validating the recovery code and clearing all sessions and
     * therefore, signing out the user on all sessions.
     *
     * @param exchange The server web exchange containing the request and response.
     * @param recoveryCode The recovery code to validate.
     *
     * @throws AuthException If the recovery code is invalid.
     *
     * @return The user document after recovery.
     */
    suspend fun recoverUser(exchange: ServerWebExchange, recoveryCode: String): UserDocument {
        logger.debug { "Recovering user and clearing all sessions" }

        val userId = try {
            authorizationService.getCurrentUserId()
        } catch (_: Exception) {
            loginTokenService.extract(exchange).userId
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
    suspend fun disable(exchange: ServerWebExchange, req: DisableTwoFactorRequest): UserResponse {
        logger.debug { "Disabling 2FA" }

        stepUpTokenService.extract(exchange)

        val user = authorizationService.getCurrentUser()

        if (!hashService.checkBcrypt(req.password, user.password)) {
            throw AuthException("Password is wrong")
        }

        user.disableTwoFactorAuth()

        val savedUser = userService.save(user)

        return userMapper.toResponse(savedUser)
    }
}
