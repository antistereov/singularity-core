package io.stereov.singularity.auth.oauth2.component

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.SessionTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2ProviderConnectedException
import io.stereov.singularity.auth.oauth2.model.CustomState
import io.stereov.singularity.auth.oauth2.model.token.OAuth2TokenType
import io.stereov.singularity.auth.oauth2.properties.OAuth2Properties
import io.stereov.singularity.auth.oauth2.service.OAuth2AuthenticationService
import io.stereov.singularity.global.exception.model.MissingRequestParameterException
import io.stereov.singularity.user.core.exception.model.EmailAlreadyExistsException
import kotlinx.coroutines.reactor.mono
import org.apache.http.client.utils.URIBuilder
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import reactor.core.publisher.Mono
import java.net.URI

class CustomOAuth2AuthenticationSuccessHandler(
    private val objectMapper: ObjectMapper,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val sessionTokenService: SessionTokenService,
    private val oAuth2AuthenticationService: OAuth2AuthenticationService,
    private val cookieCreator: CookieCreator,
    private val oAuth2Properties: OAuth2Properties,
    private val stepUpTokenService: StepUpTokenService
) : ServerAuthenticationSuccessHandler {

    private val logger = KotlinLogging.logger {}

    override fun onAuthenticationSuccess(
        exchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> = mono {
        val oauth2Token = authentication as OAuth2AuthenticationToken
        val stateValue = exchange.exchange.request.queryParams.getFirst("state")
            ?: throw MissingRequestParameterException("No state parameter provided")
        val state = objectMapper.readValue(stateValue, CustomState::class.java)

        val sessionTokenValue = state.sessionTokenValue
            ?: exchange.exchange.request.cookies.getFirst(SessionTokenType.Session.COOKIE_NAME)?.value
            ?: throw MissingRequestParameterException("No session token provided")
        val sessionToken = sessionTokenService.extract(sessionTokenValue)

        val oauth2ProviderConnectionToken = state.oauth2ProviderConnectionTokenValue
            ?: exchange.exchange.request.cookies.getFirst(OAuth2TokenType.ProviderConnection.COOKIE_NAME)?.value

        val user = oAuth2AuthenticationService.findOrCreateUser(oauth2Token, oauth2ProviderConnectionToken)
        val accessToken = accessTokenService.create(user.id, sessionToken.id)
        val refreshToken = refreshTokenService.create(user.id, sessionToken.toSessionInfoRequest(), exchange.exchange)

        val response = exchange.exchange.response
        response.headers.add(SessionTokenType.Access.HEADER, accessToken.value)
        response.headers.add(SessionTokenType.Refresh.HEADER, refreshToken.value)
        response.cookies.add(SessionTokenType.Access.COOKIE_NAME, cookieCreator.createCookie(accessToken))
        response.cookies.add(SessionTokenType.Refresh.COOKIE_NAME, cookieCreator.createCookie(refreshToken))

        if (state.stepUp) {
            val stepUpToken = stepUpTokenService.create(user.id, sessionToken.id)
            response.headers.add(SessionTokenType.StepUp.HEADER, stepUpToken.value)
            response.cookies.add(SessionTokenType.StepUp.COOKIE_NAME, cookieCreator.createCookie(stepUpToken))
        }

        if (state.redirectUri != null) {
            response.statusCode = HttpStatus.FOUND
            response.headers.location = URI(state.redirectUri)
        } else {
            response.statusCode = HttpStatus.OK
        }
    }.onErrorResume { e ->
        val errorCode = when (e) {
            is EmailAlreadyExistsException -> "email_exists"
            is OAuth2ProviderConnectedException -> "client_connected"
            is TokenExpiredException -> "token_expired"
            is InvalidTokenException -> "invalid_token"
            else -> "server_error"
        }
        logger.debug(e) { "OAuth2 authorization failed" }

        val response = exchange.exchange.response
        response.statusCode = HttpStatus.FOUND
        response.headers.location = URIBuilder(oAuth2Properties.errorRedirectUri)
            .addParameter("error", errorCode)
            .build()

        Mono.empty()
    }.then()
}
