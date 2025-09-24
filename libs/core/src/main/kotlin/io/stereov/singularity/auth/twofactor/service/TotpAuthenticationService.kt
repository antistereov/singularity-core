package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.TwoFactorMethodDisabledException
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.exception.model.CannotDisableOnly2FAMethodException
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorCodeException
import io.stereov.singularity.auth.twofactor.exception.model.MissingPasswordIdentityException
import io.stereov.singularity.auth.twofactor.exception.model.TwoFactorMethodAlreadyEnabledException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.auth.twofactor.properties.TotpRecoveryCodeProperties
import io.stereov.singularity.auth.twofactor.service.token.TotpSetupTokenService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class TotpAuthenticationService(
    private val totpService: TotpService,
    private val authorizationService: AuthorizationService,
    private val totpRecoveryCodeProperties: TotpRecoveryCodeProperties,
    private val setupTokenService: TotpSetupTokenService,
    private val hashService: HashService,
    private val userService: UserService,
    private val accessTokenCache: AccessTokenCache,
    private val userMapper: UserMapper,
    private val twoFactorAuthTokenService: TwoFactorAuthenticationTokenService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Sets up two-factor authentication for the current user.
     * It generates a secret key, an OTP auth URL, a recovery code, and a token.
     * The token is used to validate the setup process and enable two-factor authentication for the current user.
     *
     * It needs a valid two factor setup init token to perform this action.
     *
     * @return A [TwoFactorSetupResponse] containing the secret, OTP auth URL, recovery code and setup token.
     */
    suspend fun getTotpDetails(): TwoFactorSetupResponse {
        logger.debug { "Setting up two factor authentication" }

        val user = authorizationService.getCurrentUser()
        authorizationService.requireStepUp()
        if (!user.sensitive.identities.containsKey(IdentityProvider.PASSWORD)) {
            throw MissingPasswordIdentityException("Cannot set up TOTP: user did not configured sign in using password.")
        }
        if (user.sensitive.security.twoFactor.totp.enabled)
            throw TwoFactorMethodAlreadyEnabledException("The user already set up TOTP")

        val secret = totpService.generateSecretKey()
        val otpAuthUrl = totpService.getOtpAuthUrl(user.requireNotGuestAndGetEmail(), secret)
        val recoveryCodes = List(totpRecoveryCodeProperties.count) {
            Random.generateString(totpRecoveryCodeProperties.length)
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

        if (!user.sensitive.identities.containsKey(IdentityProvider.PASSWORD)) {
            throw MissingPasswordIdentityException("Cannot set up TOTP: user did not configured sign in using password.")
        }

        if (user.sensitive.security.twoFactor.totp.enabled)
            throw TwoFactorMethodAlreadyEnabledException("The user already set up TOTP")

        authorizationService.requireStepUp()

        if (!totpService.codeIsValid(setupToken.secret, code)) {
            throw AuthException("Invalid two-factor authentication code")
        }

        val encryptedSecret = setupToken.secret
        val hashedRecoveryCodes = setupToken.recoveryCodes.map {
            hashService.hashBcrypt(it)
        }

        user.setupTotp(encryptedSecret, hashedRecoveryCodes)
            .clearSessions()

        val savedUser = userService.save(user)
        accessTokenCache.invalidateAllTokens(user.id)

        return userMapper.toResponse(savedUser)
    }

    /**
     * Validates the two-factor code for the given user. It throws an exception if the code is invalid.
     *
     * @param user The user to validate the code for.
     * @param code The two-factor code to validate.
     *
     * @throws InvalidDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws InvalidTwoFactorCodeException If the two-factor code is invalid.
     */
    suspend fun validateCode(user: UserDocument, code: Int): UserDocument {
        if (!user.sensitive.security.twoFactor.totp.enabled)
            throw TwoFactorMethodDisabledException(TwoFactorMethod.TOTP)

        val secret = user.sensitive.security.twoFactor.totp.secret
            ?: throw InvalidDocumentException("TOTP is enabled for user ${user.id} but no TOTP secret was found")

        if (totpService.codeIsValid(secret, code)) return user

        throw InvalidTwoFactorCodeException()
    }

    suspend fun recoverUser(exchange: ServerWebExchange, recoveryCode: String): UserDocument {
        logger.debug { "Recovering user" }

        val userId = twoFactorAuthTokenService.extract(exchange).userId

        val user = userService.findById(userId)
        if (!user.twoFactorMethods.contains(TwoFactorMethod.TOTP))
            throw TwoFactorMethodDisabledException(TwoFactorMethod.TOTP)

        val recoveryCodeHashes = user.sensitive.security.twoFactor.totp.recoveryCodes

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
    suspend fun disable(): UserResponse {
        logger.debug { "Disabling 2FA" }

        authorizationService.requireStepUp()
        val user = authorizationService.getCurrentUser()

        if (!user.sensitive.security.twoFactor.totp.enabled) {
            throw TwoFactorMethodDisabledException(TwoFactorMethod.TOTP)
        }

        if (user.twoFactorMethods.size == 1 && user.sensitive.security.twoFactor.totp.enabled)
            throw CannotDisableOnly2FAMethodException("Failed to disable TOTP: it not allowed to disable the only configured 2FA method.")

        user.disableTotp()

        val savedUser = userService.save(user)

        return userMapper.toResponse(savedUser)
    }

}
