package io.stereov.singularity.auth.twofactor.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorAuthenticationToken
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class TwoFactorAuthenticationTokenService(
    private val jwtProperties: JwtProperties,
    private val jwtService: JwtService,
    private val tokenValueExtractor: TokenValueExtractor
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = TwoFactorTokenType.Authentication

    suspend fun create(userId: ObjectId, expiration: Long = jwtProperties.expiresIn): TwoFactorAuthenticationToken {
        logger.debug { "Creating two factor token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId.toHexString())
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return TwoFactorAuthenticationToken(userId, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange): TwoFactorAuthenticationToken {
        val tokenValue = tokenValueExtractor.extractValue(exchange, tokenType)

       return extract(tokenValue)
    }

    suspend fun extract(tokenValue: String): TwoFactorAuthenticationToken {
        logger.debug { "Extracting two factor login token" }

        val jwt = jwtService.decodeJwt(tokenValue, true)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("JWT does not contain sub")

        return TwoFactorAuthenticationToken(userId, jwt)
    }
}
