package io.stereov.singularity.auth.oauth2.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.model.token.OAuth2ProviderConnectionToken
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.user.core.model.AccountDocument
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
class OAuth2ProviderConnectionTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "oauth2_provider_connection"

    suspend fun create(userId: ObjectId, sessionId: UUID, provider: String, issuedAt: Instant = Instant.now()): OAuth2ProviderConnectionToken {
        logger.debug { "Creating OAuth2ProviderConnectionToken for user $userId and session $sessionId" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toString())
            .claim(Constants.JWT_SESSION_CLAIM, sessionId)
            .claim(Constants.JWT_OAUTH2_PROVIDER_CLAIM, provider)
            .build()

        val jwt = jwtService.encodeJwt(claims, tokenType)

        return OAuth2ProviderConnectionToken(userId, sessionId, provider, jwt)
    }

    suspend fun extract(tokenValue: String, currentUser: AccountDocument): OAuth2ProviderConnectionToken {
        logger.debug { "Extracting OAuth2ProviderConnectionToken" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("OAuth2ProviderConnectionToken does not contain sub")

        val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
            ?.let { UUID.fromString(it) }
            ?: throw InvalidTokenException("OAuth2ProviderConnectionToken does not contain session id")

        val provider = jwt.claims[Constants.JWT_OAUTH2_PROVIDER_CLAIM] as? String
            ?: throw InvalidTokenException("OAuth2ProviderConnectionToken does not contain provider")

        // Check if the refresh token is linked to a session
        if (!currentUser.sensitive.sessions.containsKey(sessionId)) {
            throw InvalidTokenException("OAuth2ProviderConnectionToken does not correspond to an existing session")
        }

        return OAuth2ProviderConnectionToken(userId, sessionId, provider, jwt)
    }
}
