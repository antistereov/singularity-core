package io.stereov.singularity.auth.core.filter

import io.stereov.singularity.auth.core.model.token.CustomAuthenticationToken
import io.stereov.singularity.auth.core.model.token.ErrorAuthenticationToken
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.jwt.exception.TokenException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class AuthenticationFilter(
    private val accessTokenService: AccessTokenService,
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain) = mono {
        val accessToken = try {
            accessTokenService.extract(exchange)
        } catch(e: TokenException) {
            return@mono setSecurityContext(chain, exchange, e)
        }

        val authentication = CustomAuthenticationToken(
            accessToken.userId,
            accessToken.roles,
            accessToken.groups,
            accessToken.sessionId,
            accessToken.tokenId,
            exchange
        )

        val securityContext = SecurityContextImpl(authentication)
        return@mono chain.filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
            .awaitFirstOrNull()
    }

    private suspend fun setSecurityContext(chain: WebFilterChain, exchange: ServerWebExchange, exception: Exception): Void? {
        val auth = ErrorAuthenticationToken(exception)
        val securityContext = SecurityContextImpl(auth)

        return chain
            .filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
            .awaitFirstOrNull()
    }
}
