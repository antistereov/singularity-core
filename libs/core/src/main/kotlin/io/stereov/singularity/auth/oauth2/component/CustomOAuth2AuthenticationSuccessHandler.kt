package io.stereov.singularity.auth.oauth2.component

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.getOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.LoginAlertService
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.oauth2.exception.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.oauth2.properties.OAuth2Properties
import io.stereov.singularity.auth.oauth2.service.OAuth2AuthenticationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.exception.OAuth2StateTokenExtractionException
import io.stereov.singularity.auth.token.exception.SessionTokenExtractionException
import io.stereov.singularity.auth.token.model.OAuth2StateToken
import io.stereov.singularity.auth.token.model.OAuth2TokenType
import io.stereov.singularity.auth.token.model.SessionToken
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.token.service.*
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.util.getOrNull
import kotlinx.coroutines.reactor.mono
import org.apache.http.client.utils.URIBuilder
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*

class CustomOAuth2AuthenticationSuccessHandler(
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val sessionTokenService: SessionTokenService,
    private val oAuth2AuthenticationService: OAuth2AuthenticationService,
    private val cookieCreator: CookieCreator,
    private val oAuth2Properties: OAuth2Properties,
    private val stepUpTokenService: StepUpTokenService,
    private val oAuth2StateTokenService: OAuth2StateTokenService,
    private val emailProperties: EmailProperties,
    private val securityAlertProperties: SecurityAlertProperties,
    private val loginAlertService: LoginAlertService
) : ServerAuthenticationSuccessHandler {

    private val logger = logger {}

    /**
     * Handles the successful authentication of a user during an OAuth2 authentication flow.
     * This method processes the authentication result, extracts necessary state and session tokens,
     * and builds the appropriate response, such as setting cookies or handling redirects.
     *
     * @param exchange the web filter exchange containing the current server web exchange and response.
     * @param authentication the authentication object representing the authenticated user or entity.
     * @return a `Mono<Void>` signaling the completion of the successful authentication process.
     */
    override fun onAuthenticationSuccess(
        exchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> = mono {
        logger.debug { "Received callback from ${exchange.exchange.request.remoteAddress}" }
        val state = getState(exchange.exchange)
        val sessionToken = getSessionToken(exchange.exchange)

        buildResponse(authentication, exchange.exchange, sessionToken, state)
    }.onErrorResume { e ->
        handleError(e, exchange.exchange)
        Mono.empty()
    }.then()

    /**
     * Retrieves and validates the OAuth2 state token from the provided server web exchange.
     * This method checks the request query parameters for the "state" parameter, validates it,
     * and extracts the associated `OAuth2StateToken`. If the state parameter is absent or
     * invalid, an appropriate exception is thrown.
     *
     * @param exchange the server web exchange containing the request details and query parameters.
     * @return the extracted and validated OAuth2 state token.
     * @throws OAuth2FlowException if the state parameter is missing, expired, or invalid.
     */
    private suspend fun getState(exchange: ServerWebExchange): OAuth2StateToken {
        val stateValue = exchange.request.queryParams.getFirst("state")
            ?: throw OAuth2FlowException(OAuth2ErrorCode.STATE_PARAMETER_MISSING,"No state parameter provided.")
        return oAuth2StateTokenService.extract(stateValue)
            .getOrElse { exception ->
                when (exception) {
                    is OAuth2StateTokenExtractionException.Expired -> throw OAuth2FlowException(OAuth2ErrorCode.STATE_EXPIRED, "State expired.", exception)
                    else -> throw OAuth2FlowException(OAuth2ErrorCode.INVALID_STATE, "State is invalid.", exception)
                }
            }
    }

    /**
     * Extracts and validates a session token from the provided web exchange.
     *
     * This method retrieves the session token from a user's browser cookies in the server web exchange.
     * The method will throw an exception if the session token is not present, invalid, or expired.
     *
     * @param exchange the server web exchange containing the incoming request and cookies.
     * @return the extracted and validated session token.
     * @throws OAuth2FlowException if the session token is missing, invalid, or expired.
     */
    private suspend fun getSessionToken(exchange: ServerWebExchange): SessionToken {
        logger.debug { "Extracting session token from callback" }

        val sessionTokenValue = exchange.request.cookies.getFirst(SessionTokenType.Session.COOKIE_NAME)?.value
            ?: throw OAuth2FlowException(OAuth2ErrorCode.SESSION_TOKEN_MISSING, "No session token provided as query parameter or cookie.")
        return sessionTokenService.extract(sessionTokenValue)
            .getOrElse { e ->
                when (e) {
                    is SessionTokenExtractionException.Expired -> throw OAuth2FlowException(OAuth2ErrorCode.SESSION_TOKEN_EXPIRED,
                        "The provided session token is expired.")
                    else -> throw OAuth2FlowException(OAuth2ErrorCode.INVALID_SESSION_TOKEN,"The provided session token cannot be decoded.")
                }
         }
    }

    /**
     * Builds the response after successful authentication in an OAuth2 flow.
     *
     * This method creates necessary tokens (access, refresh, and step-up if applicable), sets cookies,
     * handles user creation or retrieval, sends login alerts if configured, and sets either a redirection
     * or a success response depending on the OAuth2 flow configuration.
     *
     * @param authentication the authentication object representing the authenticated user or entity.
     * @param exchange the server web exchange containing the HTTP request and response.
     * @param sessionToken the session token containing session metadata and JWT token details.
     * @param state the OAuth2 state token containing redirect URI, step-up flag, and other state details.
     * @throws OAuth2FlowException if an error occurs during token creation or user retrieval.
     */
    private suspend  fun buildResponse(
        authentication: Authentication,
        exchange: ServerWebExchange,
        sessionToken: SessionToken,
        state: OAuth2StateToken
    ) {
        val oauth2Authentication = authentication as OAuth2AuthenticationToken
        val sessionId = accessTokenService.extract(exchange).getOrNull()?.let {
            when (it) {
                is AuthenticationOutcome.Authenticated -> it.sessionId
                is AuthenticationOutcome.None -> null
            }
        } ?: UUID.randomUUID()

        val oauth2ProviderConnectionToken = exchange.request.cookies.getFirst(OAuth2TokenType.ProviderConnection.COOKIE_NAME)?.value
        val stepUpTokenValue = exchange.request.cookies.getFirst(SessionTokenType.StepUp.COOKIE_NAME)?.value

        val (user, action) = oAuth2AuthenticationService.findOrCreateUser(
            oauth2Authentication,
            oauth2ProviderConnectionToken,
            stepUpTokenValue,
            exchange,
            state.stepUp,
            sessionId,
            sessionToken.locale
        )
        val accessToken = accessTokenService.create(user, sessionId)
            .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to create AccessToken")}
        val refreshToken = refreshTokenService.create(user, sessionId, sessionToken.toSessionInfoRequest(), exchange)
            .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to create RefreshToken")}

        if (state.stepUp) {
            val userId = user.id.getOrElse { throw OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to extract user ID") }
            if (userId != state.userId)
                throw OAuth2FlowException(
                    OAuth2ErrorCode.WRONG_ACCOUNT_AUTHENTICATED,
                    "Step-up failed: the account you authenticated via OAuth2 doesn't match the AccessToken"
                )
            val stepUpToken = stepUpTokenService.create(userId, sessionId)
                .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to create StepUpToken") }
            val stepUpCookie = cookieCreator.createCookie(stepUpToken)
                .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to create StepUpToken cookie") }
            exchange.response.headers.add(SessionTokenType.StepUp.HEADER, stepUpToken.value)
            exchange.response.cookies.add(SessionTokenType.StepUp.COOKIE_NAME, stepUpCookie)
        }

        val accessTokenCookie = cookieCreator.createCookie(accessToken)
            .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to create AccessToken cookie") }
        val refreshTokenCookie = cookieCreator.createCookie(refreshToken)
            .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to create RefreshToken cookie") }

        exchange.response.headers.add(SessionTokenType.Access.HEADER, accessToken.value)
        exchange.response.headers.add(SessionTokenType.Refresh.HEADER, refreshToken.value)
        exchange.response.cookies.add(SessionTokenType.Access.COOKIE_NAME, accessTokenCookie)
        exchange.response.cookies.add(SessionTokenType.Refresh.COOKIE_NAME, refreshTokenCookie)

        if (action == OAuth2AuthenticationService.OAuth2Action.LOGIN && !state.stepUp
            && emailProperties.enable && securityAlertProperties.login) {
            val session = user.sensitive.sessions[sessionId]
                ?: throw OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Current session is not saved")
            loginAlertService.send(user, sessionToken.locale, session)
        }

        if (state.redirectUri != null) {
            exchange.response.statusCode = HttpStatus.FOUND
            exchange.response.headers.location = URI(state.redirectUri)
        } else {
            exchange.response.statusCode = HttpStatus.OK
        }
    }

    /**
     * Handles errors that occur during the OAuth2 authorization flow and redirects the user to an error page.
     *
     * This method logs the encountered error, determines the appropriate error code based on the type of exception,
     * and redirects the user to a predefined error page with the error code as a query parameter.
     *
     * @param e the exception representing the error that occurred during the OAuth2 authorization flow.
     * @param exchange the server web exchange containing the HTTP request and response.
     */
    private fun handleError(e: Throwable, exchange: ServerWebExchange) {
        logger.debug(e) { "OAuth2 authorization failed" }

        val errorCode = when (e) {
            is OAuth2FlowException -> e.errorCode
            else -> OAuth2ErrorCode.SERVER_ERROR
        }

        val response = exchange.response
        response.statusCode = HttpStatus.FOUND
        response.headers.location = URIBuilder(oAuth2Properties.errorRedirectUri)
            .addParameter("code", errorCode.value)
            .build()
    }
}
