package io.stereov.singularity.auth.core.filter

import com.github.michaelbull.result.getOrElse
import io.stereov.singularity.auth.core.exception.AccessTokenException
import io.stereov.singularity.auth.core.model.token.AccessTokenExceptionToken
import io.stereov.singularity.auth.core.model.token.AuthenticationToken
import io.stereov.singularity.auth.core.service.token.AccessTokenService
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
        val accessToken = accessTokenService.extract(exchange).getOrElse { e ->
            return@mono handleAccessTokenException(chain, exchange, e)
        }

        val authentication = AuthenticationToken(
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

    private suspend fun handleAccessTokenException(
        chain: WebFilterChain,
        exchange: ServerWebExchange,
        exception: AccessTokenException
    ): Void? {
        val auth = AccessTokenExceptionToken(exception)
        val securityContext = SecurityContextImpl(auth)

        return chain
            .filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
            .awaitFirstOrNull()
    }
}
