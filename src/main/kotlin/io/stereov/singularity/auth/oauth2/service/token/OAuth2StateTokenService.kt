package io.stereov.singularity.auth.oauth2.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.model.token.OAuth2StateToken
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class OAuth2StateTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "oauth2_state"
    private val randomStateClaim = "random_state"
    private val sessionTokenClaim = "session_token"
    private val redirectUriClaim = "redirect_uri"
    private val providerConnectionTokenClaim = "oauth2_provider_connection_token"
    private val stepUpClaim = "step_up"

    suspend fun create(
        randomState: String,
        sessionTokenValue: String?,
        redirectUri: String?,
        oauth2ProviderConnectionTokenValue: String?,
        stepUp: Boolean,
    ): OAuth2StateToken {
        logger.debug { "Creating OAuth2StateToken" }

        val claims = JwtClaimsSet.builder()
            .claim(randomStateClaim, randomState)
            .claim(stepUpClaim, stepUp.toString())
            .expiresAt(Instant.now().plusSeconds(jwtProperties.expiresIn))

        if (sessionTokenValue != null)
            claims.claim(sessionTokenClaim, sessionTokenValue)
        if (redirectUri != null)
            claims.claim(redirectUriClaim, redirectUri)
        if (oauth2ProviderConnectionTokenValue != null)
            claims.claim(providerConnectionTokenClaim, oauth2ProviderConnectionTokenValue)

        val jwt = jwtService.encodeJwt(claims.build(), tokenType)

        return OAuth2StateToken(
            randomState = randomState,
            sessionTokenValue = sessionTokenValue,
            redirectUri = redirectUri,
            oauth2ProviderConnectionTokenValue = oauth2ProviderConnectionTokenValue,
            stepUp = stepUp,
            jwt= jwt
        )
    }

    suspend fun extract(tokenValue: String): OAuth2StateToken {
        logger.debug { "Extracting OAuth2StateToken" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType)

        val randomState = (jwt.claims[randomStateClaim] as? String)
            ?: throw InvalidTokenException("No random state claim found in token")
        val sessionTokenValue = jwt.claims[sessionTokenClaim]?.let { it as? String }
        val redirectUri = jwt.claims[redirectUriClaim]?.let { it as? String }
        val connectionToken = jwt.claims[providerConnectionTokenClaim]?.let { it as? String }
        val stepUp = (jwt.claims[stepUpClaim] as? String)?.toBooleanStrictOrNull()
            ?: throw InvalidTokenException("No step-up claim found in token")

        return OAuth2StateToken(
            randomState = randomState,
            sessionTokenValue = sessionTokenValue,
            redirectUri = redirectUri,
            oauth2ProviderConnectionTokenValue = connectionToken,
            stepUp = stepUp,
            jwt = jwt
        )
    }
}
