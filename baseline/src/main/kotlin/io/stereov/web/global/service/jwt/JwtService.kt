package io.stereov.web.global.service.jwt

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.jwt.exception.model.TokenExpiredException
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * # JWT Service
 *
 * This service is responsible for encoding and decoding JWT tokens.
 * It uses the `ReactiveJwtDecoder` and `JwtEncoder` to handle JWT operations.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class JwtService(
    private val jwtDecoder: ReactiveJwtDecoder,
    private val jwtEncoder: JwtEncoder,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Decodes a JWT token.
     *
     * This method decodes the JWT token and checks for expiration if specified.
     *
     * @param token The JWT token to decode.
     * @param checkExpiration Whether to check for expiration.
     *
     * @return The decoded JWT.
     */
    suspend fun decodeJwt(token: String, checkExpiration: Boolean): Jwt {
        logger.debug { "Decoding jwt" }

        val jwt = try {
            jwtDecoder.decode(token).awaitFirst()
        } catch(e: Exception) {
            throw InvalidTokenException("Cannot decode access token", e)
        }

        if (checkExpiration) {
            val expiresAt = jwt.expiresAt
                ?: throw InvalidTokenException("JWT does not contain expiration information")

            if (expiresAt <= Instant.now()) throw TokenExpiredException("Token is expired")
        }

        return jwt
    }

    /**
     * Encodes a JWT token.
     *
     * This method encodes the JWT token using the provided claims.
     *
     * @param claims The claims to include in the JWT.
     *
     * @return The encoded JWT token as a string.
     */
    fun encodeJwt(claims: JwtClaimsSet): String {
        val jwsHeader = JwsHeader.with { "HS256" }.build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }
}
