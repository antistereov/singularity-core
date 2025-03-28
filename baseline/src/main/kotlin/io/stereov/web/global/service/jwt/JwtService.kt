package io.stereov.web.global.service.jwt

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.jwt.exception.model.TokenExpiredException
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class JwtService(
    private val jwtDecoder: ReactiveJwtDecoder,
    private val jwtEncoder: JwtEncoder,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

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

    fun encodeJwt(claims: JwtClaimsSet): String {
        val jwsHeader = JwsHeader.with { "HS256" }.build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }
}
