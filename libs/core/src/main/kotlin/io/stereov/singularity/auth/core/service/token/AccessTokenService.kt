package io.stereov.singularity.auth.core.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.model.token.AccessToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

@Service
class AccessTokenService(
    private val jwtService: JwtService,
    private val accessTokenCache: AccessTokenCache,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.Access

    suspend fun create(user: UserDocument, sessionId: UUID, issuedAt: Instant = Instant.now()): AccessToken {
        logger.debug { "Creating access token for user ${user.id} and session $sessionId" }

        val tokenId = Random.generateString(20)

        accessTokenCache.addTokenId(user.id, sessionId, tokenId)

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(user.id.toHexString())
            .claim(Constants.JWT_ROLES_CLAIM, user.roles)
            .claim(Constants.JWT_SESSION_CLAIM, sessionId)
            .claim(Constants.JWT_GROUPS_CLAIM, user.groups)
            .id(tokenId)
            .build()

        val jwt = jwtService.encodeJwt(claims,tokenType.cookieName)

        return AccessToken(user.id, sessionId, tokenId, user.roles, user.groups, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange): AccessToken {

        val tokenValue = tokenValueExtractor.extractValue(exchange, tokenType, true)

        return extract(tokenValue)
    }

    suspend fun extractOrNull(exchange: ServerWebExchange): AccessToken? {

        val tokenValue = tokenValueExtractor.extractValueOrNull(exchange, tokenType, true)

        return tokenValue?.let { extract(tokenValue) }
    }

    suspend fun extractOrOAuth2FlowException(exchange: ServerWebExchange): AccessToken {

        return runCatching { extractOrNull(exchange) }
            .getOrElse { exception ->
                when(exception) {
                    is TokenExpiredException -> throw OAuth2FlowException(
                        OAuth2ErrorCode.ACCESS_TOKEN_EXPIRED,
                        "Failed to connect a new provider to the current user. AccessToken expired.",
                        exception
                    )
                    else -> throw OAuth2FlowException(
                        OAuth2ErrorCode.INVALID_ACCESS_TOKEN,
                        "Failed to connect a new provider to the current user: AccessToken is invalid",
                        exception
                    )
                }
            } ?: throw OAuth2FlowException(
            OAuth2ErrorCode.ACCESS_TOKEN_MISSING,
            "Failed to connect a new provider to the current user: AccessToken is invalid"
        )
    }

    suspend fun extract(tokenValue: String): AccessToken {
        logger.debug { "Extracting and validating access token" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType.cookieName)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("AccessToken does not contain sub")

        val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
            ?.let { UUID.fromString(it) }
            ?: throw InvalidTokenException("AccessToken does not contain session id")

        val tokenId = jwt.id
            ?: throw InvalidTokenException("AccessToken does not contain token id")

        val isValid = accessTokenCache.isTokenIdValid(userId, sessionId, tokenId)

        val roles = (jwt.claims[Constants.JWT_ROLES_CLAIM] as? List<*>)
            ?.map { role ->
                (role as? String)?.let { Role.fromString(it)}
                    ?: throw InvalidTokenException("Cannot decode role $role")
            }
            ?.toSet()
            ?: throw InvalidTokenException("AccessToken does not contain roles")

        val groups = (jwt.claims[Constants.JWT_GROUPS_CLAIM] as? List<*>)
            ?.map { it as? String ?: throw InvalidTokenException("Cannot decode group $it") }
            ?.toSet()
            ?: throw InvalidTokenException("AccessToken does not contain groups")

        if (!isValid) {
            throw InvalidTokenException("AccessToken is not valid")
        }

        return AccessToken(userId, sessionId, tokenId, roles, groups, jwt)
    }
}
