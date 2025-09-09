package io.stereov.singularity.auth.session.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.session.cache.AccessTokenCache
import io.stereov.singularity.auth.session.model.AccessToken
import io.stereov.singularity.auth.session.model.SessionTokenType
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class AccessTokenService(
    private val jwtService: JwtService,
    private val accessTokenCache: AccessTokenCache,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.Access

    suspend fun create(userId: ObjectId, deviceId: String, issuedAt: Instant = Instant.now()): AccessToken {
        logger.debug { "Creating access token for user $userId and device $deviceId" }

        val tokenId = Random.generateCode(20)

        accessTokenCache.addTokenId(userId, tokenId)

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toHexString())
            .claim(Constants.JWT_DEVICE_CLAIM, deviceId)
            .id(tokenId)
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return AccessToken(userId, deviceId, tokenId, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange): AccessToken {
        logger.debug { "Extracting and validating access token" }

        val token = tokenValueExtractor.extractValue(exchange, tokenType, true)

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("JWT does not contain sub")

        val deviceId = jwt.claims[Constants.JWT_DEVICE_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain device id")

        val tokenId = jwt.id
            ?: throw InvalidTokenException("JWT does not contain token id")

        val isValid = accessTokenCache.isTokenIdValid(userId, tokenId)

        if (!isValid) {
            throw InvalidTokenException("Access token is not valid")
        }

        return AccessToken(userId, deviceId, tokenId, jwt)
    }
}
