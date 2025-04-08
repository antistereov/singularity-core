package io.stereov.web.user.service.token

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.config.Constants
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.properties.JwtProperties
import io.stereov.web.user.service.token.model.StepUpToken
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
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
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Creates a two-factor authentication token for the given user ID.
     *
     * @param userId The ID of the user.
     * @param expiration The expiration time in seconds. Default is the configured expiration time.
     *
     * @return The generated two-factor authentication token.
     */
    fun createTwoFactorToken(userId: String, expiration: Long = jwtProperties.expiresIn): String {
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
    suspend fun validateTwoFactorTokenAndExtractUserId(token: String): String {
        logger.debug { "Validating two factor token" }

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        return userId
    }

    /**
     * Creates a step-up token for currently logged-in user.
     *
     * @param issuedAt The time the token is issued. Default is the current time.
     *
     * @return The generated step-up token.
     */
    suspend fun createStepUpToken(code: Int, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating step up token" }

        val userId = authenticationService.getCurrentUserId()
        val deviceId = authenticationService.getCurrentDeviceId()

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
}
