package io.stereov.singularity.auth.core.filter

import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.model.CustomAuthenticationToken
import io.stereov.singularity.auth.core.model.ErrorAuthenticationToken
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.session.service.AccessTokenService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.user.core.service.UserService
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
    private val userService: UserService,
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain) = mono {
        val accessToken = try {
            accessTokenService.extract(exchange)
        } catch(e: TokenException) {
            return@mono setSecurityContext(chain, exchange, e)
        }

        val user = try {
            userService.findById(accessToken.userId)
        } catch (_: DocumentNotFoundException) {
            val authException = AuthException("Invalid access token: user does not exist")
            return@mono setSecurityContext(chain, exchange, authException)
        }

        if (!user.sensitive.devices.any { it.id == accessToken.deviceId }) {
            val e = InvalidTokenException("Trying to login from invalid device")
            return@mono setSecurityContext(chain, exchange, e)
        }

        val authentication = CustomAuthenticationToken(user, accessToken.deviceId, accessToken.tokenId)

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
