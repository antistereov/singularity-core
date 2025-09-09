package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.model.TwoFactorLoginToken
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class TwoFactorLoginTokenService(
    private val jwtProperties: JwtProperties,
    private val jwtService: JwtService,
    private val tokenValueExtractor: TokenValueExtractor
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = TwoFactorTokenType.Login

    suspend fun create(userId: ObjectId, expiration: Long = jwtProperties.expiresIn): TwoFactorLoginToken {
        logger.debug { "Creating two factor token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId.toHexString())
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return TwoFactorLoginToken(userId, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange): TwoFactorLoginToken {
        logger.debug { "Extracting two factor login token" }

        val token = tokenValueExtractor.extractValue(exchange, tokenType)

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("JWT does not contain sub")

        return TwoFactorLoginToken(userId, jwt)
    }
}