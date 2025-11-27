package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.exception.OAuth2StateTokenCreationException
import io.stereov.singularity.auth.token.exception.OAuth2StateTokenExtractionException
import io.stereov.singularity.auth.token.model.OAuth2StateToken
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for managing OAuth2 state tokens. This service provides functionality to create and extract
 * OAuth2 state tokens, which are used to manage and validate the state of OAuth2 authentication processes.
 *
 * The service leverages JWT for token creation and decoding. Each token is associated with specific
 * claims, including a random state identifier, redirect URI, and additional authentication
 * requirements (step-up).
 */
@Service
class OAuth2StateTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "oauth2_state"
    private val randomStateClaim = "random_state"
    private val redirectUriClaim = "redirect_uri"
    private val stepUpClaim = "step_up"

    /**
     * Creates an [OAuth2StateToken] with the specified parameters.
     *
     * @param randomState A unique string used to ensure the state of the OAuth2 process is secure and consistent.
     * @param redirectUri The optional URI where the response will be redirected after token processing.
     * @param stepUp A flag indicating whether additional authentication steps are required.
     * @return A [Result] containing the generated [OAuth2StateToken] if successful, or an [OAuth2StateTokenCreationException] if an error occurs during the creation process.
     */
    suspend fun create(
        randomState: String,
        redirectUri: String?,
        stepUp: Boolean,
    ): Result<OAuth2StateToken, OAuth2StateTokenCreationException> = coroutineBinding {
        logger.debug { "Creating OAuth2StateToken" }

        val claims = runCatching {
            val claimsSet = JwtClaimsSet.builder()
                .claim(randomStateClaim, randomState)
                .claim(stepUpClaim, stepUp.toString())
                .expiresAt(Instant.now().plusSeconds(jwtProperties.expiresIn))


            if (redirectUri != null)
                claimsSet.claim(redirectUriClaim, redirectUri)

            claimsSet.build()
        }
            .mapError { ex -> OAuth2StateTokenCreationException.Encoding("Failed to build claim set: ${ex.message}", ex) }
            .bind()


        jwtService.encodeJwt(claims, tokenType)
            .mapError { ex -> OAuth2StateTokenCreationException.fromTokenCreationException(ex) }
            .map { jwt -> OAuth2StateToken(randomState, redirectUri, stepUp, jwt) }
            .bind()
    }

    /**
     * Extracts an [OAuth2StateToken] from the provided JWT token string.
     *
     * @param tokenValue The JWT token string to be decoded and parsed into an [OAuth2StateToken].
     * @return A [Result] containing the extracted [OAuth2StateToken] if successful, or an [OAuth2StateTokenExtractionException] if an error occurs during the extraction process.
     */
    suspend fun extract(
        tokenValue: String
    ): Result<OAuth2StateToken, OAuth2StateTokenExtractionException> = coroutineBinding {
        logger.debug { "Extracting OAuth2StateToken" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType)
            .mapError { ex -> OAuth2StateTokenExtractionException.fromTokenExtractionException(ex) }
            .bind()

        val randomState = (jwt.claims[randomStateClaim] as? String)
            .toResultOr { OAuth2StateTokenExtractionException.Invalid("No random state claim found in token") }
            .bind()
        val redirectUri = jwt.claims[redirectUriClaim]?.let { it as? String }
        val stepUp = (jwt.claims[stepUpClaim] as? String)?.toBooleanStrictOrNull()
            .toResultOr { OAuth2StateTokenExtractionException.Invalid("No step up claim found in token") }
            .bind()

        OAuth2StateToken(
            randomState = randomState,
            redirectUri = redirectUri,
            stepUp = stepUp,
            jwt = jwt,
        )
    }
}
