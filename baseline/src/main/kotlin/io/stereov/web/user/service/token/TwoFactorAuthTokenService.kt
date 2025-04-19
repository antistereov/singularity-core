package io.stereov.web.user.service.token

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.auth.exception.model.InvalidCredentialsException
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.config.Constants
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.web.global.service.twofactorauth.exception.model.InvalidTwoFactorCodeException
import io.stereov.web.properties.JwtProperties
import io.stereov.web.user.dto.request.TwoFactorStartSetupRequest
import io.stereov.web.user.exception.model.InvalidUserDocumentException
import io.stereov.web.user.service.token.model.SetupToken
import io.stereov.web.user.service.token.model.StepUpToken
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

/**
 * # Service for managing two-factor authentication tokens.
 *
 * This service provides methods to create and validate two-factor authentication tokens.
 * It uses the [JwtService] to handle JWT encoding and decoding.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class TwoFactorAuthTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val authenticationService: AuthenticationService,
    private val twoFactorAuthService: TwoFactorAuthService,
    private val hashService: HashService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Creates a setup token for two-factor authentication.
     *
     * @param userId The ID of the user.
     * @param secret The secret key for two-factor authentication.
     * @param recoveryCodes The recovery codes for two-factor authentication.
     *
     * @return The generated setup token.
     */
    fun createSetupToken(userId: String, secret: String, recoveryCodes: List<String>, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating setup token for 2fa" }

        // TODO: token nicht als cookie speichern, sondern im body einer get
        //  response schicken und body von post bekommen

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId)
            .claim(Constants.TWO_FACTOR_SECRET_CLAIM, secret)
            .claim(Constants.TWO_FACTOR_RECOVERY_CLAIM, recoveryCodes)
            .build()

        return jwtService.encodeJwt(claims)
    }

    /**
     * Validates the given setup token and extracts the two-factor authentication secret and recovery code.
     *
     * @param token The setup token to validate.
     *
     * @return A [SetupToken] object containing the secret and recovery code.
     *
     * @throws InvalidTokenException If the token is invalid or does not contain the required claims.
     */
    suspend fun validateAndExtractSetupToken(token: String): SetupToken {
        val jwt = jwtService.decodeJwt(token, true)

        val userId = authenticationService.getCurrentUserId()

        if (jwt.subject != userId) {
            throw InvalidTokenException("Setup token is not valid for current user")
        }

        val secret = jwt.claims[Constants.TWO_FACTOR_SECRET_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain valid 2fa secret")


        val recoveryCodes = jwt.claims[Constants.TWO_FACTOR_RECOVERY_CLAIM] as? List<*>
            ?: throw InvalidTokenException("JWT does not contain valid 2fa recovery codes")

        val recoveryCodeStrings = recoveryCodes
            .map { it as? String ?: throw InvalidTokenException("Recovery codes contained in JWT cannot be casted to String") }

        return SetupToken(secret, recoveryCodeStrings)
    }

    /**
     * Creates a two-factor authentication token for the given user ID.
     *
     * @param userId The ID of the user.
     * @param expiration The expiration time in seconds. Default is the configured expiration time.
     *
     * @return The generated two-factor authentication token.
     */
    fun createLoginToken(userId: String, expiration: Long = jwtProperties.expiresIn): String {
        logger.debug { "Creating two factor token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId)
            .build()

        return jwtService.encodeJwt(claims)
    }

    /**
     * Validates the given two-factor authentication token and extracts the user ID.
     *
     * @param token The two-factor authentication token to validate.
     *
     * @return The user ID extracted from the token.
     *
     * @throws InvalidTokenException If the token is invalid or does not contain the required claims.
     */
    suspend fun validateLoginTokenAndExtractUserId(token: String): String {
        logger.debug { "Validating two factor token" }

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        return userId
    }

    /**
     * Creates a step-up token based on the current user's password.
     *
     * @param code The 2FA code for the current user.
     * @param issuedAt The issue date of the token. This parameter is only used for testing. Leave it empty in production code.
     *
     * @return A step-up token for the current user.
     *
     * @throws InvalidUserDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws InvalidTwoFactorCodeException If the two-factor code is invalid.
     */
    suspend fun createStepUpToken(code: Int, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating step up token" }


        val user = authenticationService.getCurrentUser()
        val deviceId = authenticationService.getCurrentDeviceId()

        twoFactorAuthService.validateTwoFactorCode(user, code)

        return createStepUpToken(user.id, deviceId, issuedAt)
    }

    /**
     * Create a step-up token for 2FA recovery.
     *
     * @param userId The ID of the user to be recovered.
     * @param deviceId The ID of the device the user is trying to recover from.
     * @param issuedAt The time the token is issued. This is used in testing. Leave empty for production code.
     *
     * @throws AuthException If this function is called from a path that does not match `/auth/2fa/recovery`.
     */
    suspend fun createStepUpTokenForRecovery(userId: String, deviceId: String, exchange: ServerWebExchange, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating step up token" }

        if (exchange.request.path.toString() != "/user/2fa/recovery")
            throw AuthException("Cannot create step up token. This function call is only allowed when it is called from /auth/2fa/recovery")

        return createStepUpToken(userId, deviceId, issuedAt)
    }

    /**
     * Creates a step-up token for the given user ID and device ID.
     *
     * @param userId The ID of the user.
     * @param deviceId The ID of the device.
     * @param issuedAt The time the token is issued. Default is the current time.
     *
     * @return The generated step-up token.
     */
    suspend fun createStepUpToken(userId: String, deviceId: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating step up token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId)
            .claim(Constants.JWT_DEVICE_CLAIM, deviceId)
            .build()

        return jwtService.encodeJwt(claims)
    }

    /**
     * Validates the given step-up token and extracts the user ID and device ID.
     *
     * @param token The step-up token to validate.
     *
     * @return A [StepUpToken] object containing the user ID and device ID.
     *
     * @throws InvalidTokenException If the token is invalid or does not contain the required claims.
     */
    suspend fun validateAndExtractStepUpToken(token: String): StepUpToken {
        logger.debug { "Validating step up token" }

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        if (userId != authenticationService.getCurrentUserId()) {
            throw InvalidTokenException("Step up token is not valid for currently logged in user")
        }

        val deviceId = jwt.claims[Constants.JWT_DEVICE_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain device id")

        if (deviceId != authenticationService.getCurrentDeviceId()) {
            throw InvalidTokenException("Step up token is not valid for current device")
        }

        return StepUpToken(userId, deviceId)
    }

    suspend fun createSetupToken(req: TwoFactorStartSetupRequest, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating setup token" }

        val user = authenticationService.getCurrentUser()
        val deviceId = authenticationService.getCurrentDeviceId()

        if (!hashService.checkBcrypt(req.password, user.password)) throw InvalidCredentialsException("Wrong password")

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(user.id)
            .claim(Constants.JWT_DEVICE_CLAIM, deviceId)
            .build()

        return jwtService.encodeJwt(claims)
    }

    suspend fun validateSetupToken(token: String) {
        logger.debug { "Validating setup token" }

        val jwt = jwtService.decodeJwt(token,true)

        val userId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        if (userId != authenticationService.getCurrentUserId()) {
            throw InvalidTokenException("Step up token is not valid for currently logged in user")
        }

        val deviceId = jwt.claims[Constants.JWT_DEVICE_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain device id")

        if (deviceId != authenticationService.getCurrentDeviceId()) {
            throw InvalidTokenException("Step up token is not valid for current device")
        }
    }
}
