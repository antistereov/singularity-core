package io.stereov.singularity.auth.oauth2.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.model.token.OAuth2ProviderConnectionToken
import io.stereov.singularity.auth.oauth2.model.token.OAuth2TokenType
import io.stereov.singularity.global.util.Constants
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class OAuth2ProviderConnectionTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
    private val authorizationService: AuthorizationService,
) {

    private val logger = KotlinLogging.logger {}

    suspend fun create(userId: ObjectId, sessionId: String, provider: String, issuedAt: Instant = Instant.now()): OAuth2ProviderConnectionToken {
        logger.debug { "Creating OAuth2ProviderConnectionToken for user $userId and session $sessionId" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toString())
            .claim(Constants.JWT_SESSION_CLAIM, sessionId)
            .claim(Constants.JWT_OAUTH2_PROVIDER_CLAIM, provider)
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return OAuth2ProviderConnectionToken(userId, sessionId, provider, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange): OAuth2ProviderConnectionToken {
        logger.debug { "Extracting OAuth2ProviderConnectionToken" }

        val tokenValue = tokenValueExtractor.extractValue(exchange, OAuth2TokenType.Connection)

        val jwt = jwtService.decodeJwt(tokenValue, true)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("OAuth2ProviderConnectionToken does not contain sub")

        val sessionId = jwt.claims[Constants.JWT_SESSION_CLAIM] as? String
            ?: throw InvalidTokenException("OAuth2ProviderConnectionToken does not contain session id")

        val provider = jwt.claims[Constants.JWT_OAUTH2_PROVIDER_CLAIM] as? String
            ?: throw InvalidTokenException("OAuth2ProviderConnectionToken does not contain provider")

        val user = authorizationService.getCurrentUser()

        // Check if the refresh token is linked to a session
        if (user.sensitive.sessions.none { it.id == sessionId }) {
            throw InvalidTokenException("OAuth2ProviderConnectionToken does not correspond to an existing session")
        }

        return OAuth2ProviderConnectionToken(userId, sessionId, provider, jwt)
    }
}
