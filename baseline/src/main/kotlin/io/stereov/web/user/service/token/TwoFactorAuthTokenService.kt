package io.stereov.web.user.service.token

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.properties.JwtProperties
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TwoFactorAuthTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun createTwoFactorToken(userId: String, expiration: Long = jwtProperties.expiresIn): String {
        logger.debug { "Creating two factor token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId)
            .build()

        return jwtService.encodeJwt(claims)
    }

    suspend fun validateTwoFactorTokenAndExtractUserId(token: String): String {
        logger.debug { "Validating two factor token" }

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        return userId
    }
}
