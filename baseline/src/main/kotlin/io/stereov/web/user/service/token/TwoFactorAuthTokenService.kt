package io.stereov.web.user.service.token

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.properties.JwtProperties
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
}
