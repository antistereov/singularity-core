package io.stereov.singularity.auth.oauth2.component

import io.stereov.singularity.auth.oauth2.service.token.OAuth2StateTokenService
import io.stereov.singularity.global.util.Constants
import kotlinx.coroutines.reactor.mono
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class CustomOAuth2AuthorizationRequestResolver(
    clientRegistrations: ReactiveClientRegistrationRepository,
    private val oAuth2StateTokenService: OAuth2StateTokenService
) : ServerOAuth2AuthorizationRequestResolver {

    private val delegate = DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrations)

    override fun resolve(exchange: ServerWebExchange): Mono<OAuth2AuthorizationRequest> {
        return delegate.resolve(exchange)
            .flatMap { request ->
                mono { addSessionTokenToState(exchange, request) }
            }
    }

    override fun resolve(exchange: ServerWebExchange, clientRegistrationId: String): Mono<OAuth2AuthorizationRequest> {
        return delegate.resolve(exchange, clientRegistrationId)
            .flatMap { request ->
                mono { addSessionTokenToState(exchange, request) }
            }
    }

    private suspend fun addSessionTokenToState(
        exchange: ServerWebExchange,
        request: OAuth2AuthorizationRequest
    ): OAuth2AuthorizationRequest {
        val sessionToken = exchange.request.queryParams.getFirst(Constants.SESSION_TOKEN_PARAMETER)
        val redirectUri = exchange.request.queryParams.getFirst(Constants.REDIRECT_URI_PARAMETER)
        val oauth2ProviderConnectionToken = exchange.request.queryParams.getFirst(Constants.OAUTH2_PROVIDER_CONNECTION_TOKEN_PARAMETER)
        val stepUp = exchange.request.queryParams.getFirst(Constants.STEP_UP_PARAMETER).toBoolean()

        val oAuth2StateToken = oAuth2StateTokenService.create(
            request.state,
            sessionToken,
            redirectUri,
            oauth2ProviderConnectionToken,
            stepUp
        )

        val req = OAuth2AuthorizationRequest.from(request)
            .state(oAuth2StateToken.value)

        if (stepUp) {
            val additionalParameters = mutableMapOf<String, Any>()
            additionalParameters["prompt"] = "login"

            req.additionalParameters(additionalParameters)
        }

        return req.build()
    }
}
