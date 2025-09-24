package io.stereov.singularity.auth.oauth2.component

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.oauth2.properties.OAuth2Properties
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.server.DefaultServerRedirectStrategy
import org.springframework.security.web.server.ServerRedirectStrategy
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono


class CustomOAuth2AuthenticationFailureHandler(
    private val oAuth2Properties: OAuth2Properties
) : ServerAuthenticationFailureHandler {

    private val redirectStrategy: ServerRedirectStrategy = DefaultServerRedirectStrategy()

    private val logger = KotlinLogging.logger {}

    override fun onAuthenticationFailure(
        webFilterExchange: WebFilterExchange,
        exception: AuthenticationException
    ): Mono<Void> {
        logger.debug(exception) { "Authentication failed!" }

        val redirectUri = UriComponentsBuilder
            .fromUriString(oAuth2Properties.errorRedirectUri)
            .queryParam("code", OAuth2ErrorCode.AUTHENTICATION_FAILED.value)

        if (exception is OAuth2AuthenticationException) {
            redirectUri.queryParam("details", exception.error.errorCode)
        }

        return redirectStrategy.sendRedirect(webFilterExchange.exchange, redirectUri.build().toUri())
    }
}