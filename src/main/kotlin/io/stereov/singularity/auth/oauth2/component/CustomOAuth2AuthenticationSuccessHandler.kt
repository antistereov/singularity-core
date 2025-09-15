package io.stereov.singularity.auth.oauth2.component

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.model.token.SessionToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.SessionTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.CustomState
import io.stereov.singularity.auth.oauth2.model.token.OAuth2TokenType
import io.stereov.singularity.auth.oauth2.properties.OAuth2Properties
import io.stereov.singularity.auth.oauth2.service.OAuth2AuthenticationService
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
    private val objectMapper: ObjectMapper,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val sessionTokenService: SessionTokenService,
    private val oAuth2AuthenticationService: OAuth2AuthenticationService,
    private val cookieCreator: CookieCreator,
    private val oAuth2Properties: OAuth2Properties,
    private val stepUpTokenService: StepUpTokenService,
    private val authorizationService: AuthorizationService,
) : ServerAuthenticationSuccessHandler {

    private val logger = KotlinLogging.logger {}

    override fun onAuthenticationSuccess(
        exchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> = mono {
        val state = getState(exchange.exchange)
        val sessionToken = getSessionToken(state, exchange.exchange)

        buildResponse(authentication, exchange.exchange, sessionToken, state)
    }.onErrorResume { e ->
        handleError(e, exchange.exchange)
        Mono.empty()
    }.then()

    private fun getState(exchange: ServerWebExchange): CustomState {
        val stateValue = exchange.request.queryParams.getFirst("state")
            ?: throw OAuth2FlowException("state_parameter_missing","No state parameter provided.")
        return objectMapper.readValue(stateValue, CustomState::class.java)
    }

    private suspend fun getSessionToken(state: CustomState, exchange: ServerWebExchange): SessionToken {
        logger.debug { "Extracting session token from callback" }

        val sessionTokenValue = state.sessionTokenValue
            ?: exchange.request.cookies.getFirst(SessionTokenType.Session.COOKIE_NAME)?.value
            ?: throw OAuth2FlowException("session_token_missing", "No session token provided as query parameter or cookie.")
        return try {
            sessionTokenService.extract(sessionTokenValue)
        } catch (e: Exception) {
            when (e) {
                is TokenExpiredException -> throw OAuth2FlowException("session_token_expired", "The provided session token is expired.")
                else -> throw OAuth2FlowException("invalid_session_token","The provided session token cannot be decoded.")
            }
        }
    }

    private suspend  fun buildResponse(
        authentication: Authentication,
        exchange: ServerWebExchange,
        sessionToken: SessionToken,
        state: CustomState
    ) {
        val oauth2Authentication = authentication as OAuth2AuthenticationToken
        val sessionId = authorizationService.getCurrentSessionIdOrNull() ?: UUID.randomUUID()

        val oauth2ProviderConnectionToken = state.oauth2ProviderConnectionTokenValue
            ?: exchange.request.cookies.getFirst(OAuth2TokenType.ProviderConnection.COOKIE_NAME)?.value

        val user = oAuth2AuthenticationService.findOrCreateUser(oauth2Authentication, oauth2ProviderConnectionToken)
        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user.id, sessionId, sessionToken.toSessionInfoRequest(), exchange)


        exchange.response.headers.add(SessionTokenType.Access.HEADER, accessToken.value)
        exchange.response.headers.add(SessionTokenType.Refresh.HEADER, refreshToken.value)
        exchange.response.cookies.add(SessionTokenType.Access.COOKIE_NAME, cookieCreator.createCookie(accessToken))
        exchange.response.cookies.add(SessionTokenType.Refresh.COOKIE_NAME, cookieCreator.createCookie(refreshToken))

        if (state.stepUp) {
            val stepUpToken = stepUpTokenService.create(user.id, sessionId)
            exchange.response.headers.add(SessionTokenType.StepUp.HEADER, stepUpToken.value)
            exchange.response.cookies.add(SessionTokenType.StepUp.COOKIE_NAME, cookieCreator.createCookie(stepUpToken))
        }

        if (state.redirectUri != null) {
            exchange.response.statusCode = HttpStatus.FOUND
            exchange.response.headers.location = URI(state.redirectUri)
        } else {
            exchange.response.statusCode = HttpStatus.OK
        }
    }

    private fun handleError(e: Throwable, exchange: ServerWebExchange) {
        logger.debug(e) { "OAuth2 authorization failed" }

        val errorCode = when (e) {
            is OAuth2FlowException -> e.errorCode
            else -> "server_error"
        }

        val response = exchange.response
        response.statusCode = HttpStatus.FOUND
        response.headers.location = URIBuilder(oAuth2Properties.errorRedirectUri)
            .addParameter("error", errorCode)
            .build()
    }
}
