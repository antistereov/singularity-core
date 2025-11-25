package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.token.model.AuthenticationFilterExceptionToken
import io.stereov.singularity.auth.token.model.StepUpToken
import io.stereov.singularity.auth.token.service.StepUpTokenService
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

/**
 * A service responsible for managing and validating user authentication and authorization.
 *
 * This service provides methods for validating user authentication, retrieving user details
 * such as user ID, session ID, access token ID, roles, and groups. It also includes utilities
 * for enforcing access control checks such as role requirements, group membership, and step-up
 * authentication validation.
 *
 * @constructor Initializes the service with the required dependencies.
 * @param stepUpTokenService A service for managing step-up authentication tokens.
 */
@Service
class AuthorizationService(
    private val stepUpTokenService: StepUpTokenService,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Checks whether the current user is authenticated.
     *
     * @return A boolean indicating whether the user is authenticated.
     */
    suspend fun isAuthenticated(): Boolean {
        logger.debug { "Checking if user is authenticated" }

        return getAuthenticationOutcome()
            .map { outcome -> outcome.isAuthenticated }
            .getOrElse { false }
    }


    /**
     * Validates the step-up authentication process for the provided authenticated user.
     *
     * The method extracts a step-up authentication token based on the user's authentication
     * information and the current server web exchange.
     *
     * @param authentication The authenticated user's authentication details, including user ID and session ID.
     * @param exchange The server web exchange that contains the current HTTP request and response.
     * @return A [Result] containing the extracted [StepUpToken] if the extraction is successful,
     *   or a [TokenExtractionException] if an error occurs during token extraction.
     */
    suspend fun requireStepUp(
        authentication: AuthenticationOutcome.Authenticated,
        exchange: ServerWebExchange
    ): Result<StepUpToken, TokenExtractionException> {
        logger.debug { "Validating step up" }

        return stepUpTokenService.extract(
            exchange,
            authentication.userId,
            authentication.sessionId
        )
    }

    /**
     * Retrieves the outcome of the current authentication process.
     *
     * This method evaluates the authentication state stored in the reactive security context
     * and identifies whether the user is authenticated, unauthenticated, or if there was an
     * error during the authentication process.
     *
     * @return A [Result] containing:
     * - [AuthenticationOutcome] if the authentication process completed successfully.
     * - [AccessTokenExtractionException] if an error occurred during the extraction of the authentication token.
     */
    suspend fun getAuthenticationOutcome(): Result<AuthenticationOutcome, AccessTokenExtractionException> {

        val token = ReactiveSecurityContextHolder.getContext()
            .awaitFirstOrNull()?.authentication
        return when (token) {
            null -> Ok(AuthenticationOutcome.None())
            is AuthenticationOutcome -> Ok(token)
            is AuthenticationFilterExceptionToken -> Err(token.error)
            else -> Err(AccessTokenExtractionException.Invalid("Unexpected wrong SecurityContext: contains authentication type ${token::class.simpleName}"))
        }
    }
}
