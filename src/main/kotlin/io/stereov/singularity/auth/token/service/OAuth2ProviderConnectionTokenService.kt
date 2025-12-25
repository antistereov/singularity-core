package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.exception.OAuth2ProviderConnectionTokenCreationException
import io.stereov.singularity.auth.token.exception.OAuth2ProviderConnectionTokenExtractionException
import io.stereov.singularity.auth.token.model.OAuth2ProviderConnectionToken
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Service responsible for creating and extracting OAuth2 provider connection tokens.
 * These tokens are specifically designed to facilitate integration with OAuth2 providers,
 * containing information about the user, the session, and the specific provider.
 *
 * The created tokens are encoded as JSON Web Tokens (JWT) and include claims for session
 * identification and provider association. This service ensures secure creation, encoding,
 * and validation of these tokens.
 */
@Service
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
class OAuth2ProviderConnectionTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "oauth2_provider_connection"

    /**
     * Creates an [OAuth2ProviderConnectionToken] for the given principal ID, session ID, and provider.
     * This token is issued at the specified time and encoded as a JWT.
     *
     * @param principalId the unique identifier of the principal for whom the token is being created
     * @param sessionId the unique identifier of the session associated with the token
     * @param provider the OAuth2 provider for which the token is being created
     * @param issuedAt the timestamp at which the token is issued; defaults to the current time
     * @return A [Result] containing the created [OAuth2ProviderConnectionToken] on success,
     * or an [OAuth2ProviderConnectionTokenCreationException] if token creation fails
     */
    suspend fun create(
        principalId: ObjectId,
        sessionId: UUID,
        provider: String,
        issuedAt: Instant = Instant.now()
    ): Result<OAuth2ProviderConnectionToken, OAuth2ProviderConnectionTokenCreationException> = coroutineBinding {
        logger.debug { "Creating OAuth2ProviderConnectionToken for principal $principalId and session $sessionId" }

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(principalId.toString())
                .claim(Constants.JWT_SESSION_CLAIM, sessionId)
                .claim(Constants.JWT_OAUTH2_PROVIDER_CLAIM, provider)
                .build()
        }
            .mapError { ex -> OAuth2ProviderConnectionTokenCreationException.Encoding("Failed to build claim set: ${ex.message}", ex) }
            .bind()

        jwtService.encodeJwt(claims, tokenType)
            .mapError { ex -> OAuth2ProviderConnectionTokenCreationException.fromTokenCreationException(ex) }
            .map { jwt -> OAuth2ProviderConnectionToken(principalId, sessionId, provider, jwt) }
            .bind()
    }

    /**
     * Extracts an [OAuth2ProviderConnectionToken] from the given JWT token value and validates it
     * against the provided principal data, ensuring proper association with a session.
     *
     * @param tokenValue the raw JWT token value representing the OAuth2 provider connection token
     * @param principal the principal information used for validation, containing roles and sensitive data
     * @return A [Result] containing the extracted and validated [OAuth2ProviderConnectionToken],
     * or an [OAuth2ProviderConnectionTokenExtractionException] if extraction or validation fails
     */
    suspend fun extract(
        tokenValue: String,
        principal: Principal<out Role, out SensitivePrincipalData>
    ): Result<OAuth2ProviderConnectionToken, OAuth2ProviderConnectionTokenExtractionException> = coroutineBinding {
        logger.debug { "Extracting OAuth2ProviderConnectionToken" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType)
            .mapError { ex -> OAuth2ProviderConnectionTokenExtractionException.fromTokenExtractionException(ex) }
            .bind()

        val userId = jwt.subject?.let { runCatching { ObjectId(it) }.get() }
            .toResultOr { OAuth2ProviderConnectionTokenExtractionException.Invalid("OAuth2ProviderConnectionToken does not contain sub") }
            .bind()

        val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
            ?.let { UUID.fromString(it) }
            .toResultOr { OAuth2ProviderConnectionTokenExtractionException.Invalid("OAuth2ProviderConnectionToken does not contain session id") }
            .bind()

        val provider = (jwt.claims[Constants.JWT_OAUTH2_PROVIDER_CLAIM] as? String)
            .toResultOr { OAuth2ProviderConnectionTokenExtractionException.Invalid("OAuth2ProviderConnectionToken does not contain provider") }
            .bind()

        // Check if the refresh token is linked to a session
        if (!principal.sensitive.sessions.containsKey(sessionId)) {
            Err(OAuth2ProviderConnectionTokenExtractionException.Invalid("OAuth2ProviderConnectionToken does not correspond to an existing session"))
                .bind()
        }

        OAuth2ProviderConnectionToken(userId, sessionId, provider, jwt)
    }
}
