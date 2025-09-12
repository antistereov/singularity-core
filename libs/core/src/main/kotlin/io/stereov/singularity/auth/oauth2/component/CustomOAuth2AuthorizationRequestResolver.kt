package io.stereov.singularity.auth.oauth2.component

import com.fasterxml.jackson.databind.ObjectMapper
import io.stereov.singularity.auth.oauth2.model.CustomState
import io.stereov.singularity.global.exception.model.MissingRequestParameterException
import io.stereov.singularity.global.util.Constants
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class CustomOAuth2AuthorizationRequestResolver(
    clientRegistrations: ReactiveClientRegistrationRepository,
    private val objectMapper: ObjectMapper,
) : ServerOAuth2AuthorizationRequestResolver {

    private val delegate = DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrations)

    override fun resolve(exchange: ServerWebExchange): Mono<OAuth2AuthorizationRequest> {
        return delegate.resolve(exchange)
            .map { request ->
                addSessionTokenToState(exchange, request)
            }
    }

    override fun resolve(exchange: ServerWebExchange, clientRegistrationId: String): Mono<OAuth2AuthorizationRequest> {
        return delegate.resolve(exchange, clientRegistrationId)
            .map { request ->
                addSessionTokenToState(exchange, request)
            }
    }

    private fun addSessionTokenToState(
        exchange: ServerWebExchange,
        request: OAuth2AuthorizationRequest
    ): OAuth2AuthorizationRequest {
        val sessionToken = exchange.request.queryParams.getFirst(Constants.SESSION_TOKEN_PARAMETER)
            ?: throw MissingRequestParameterException("No session token found in oauth request")

        val customState = CustomState(request.state, sessionToken)
        val customStateJson = objectMapper.writeValueAsString(customState)

        return OAuth2AuthorizationRequest.from(request)
            .state(customStateJson)
            .build()
    }
}
