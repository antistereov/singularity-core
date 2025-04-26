package io.stereov.singularity.filter

import io.stereov.singularity.auth.exception.AuthException
import io.stereov.singularity.auth.model.CustomAuthenticationToken
import io.stereov.singularity.auth.model.ErrorAuthenticationToken
import io.stereov.singularity.config.Constants
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.service.jwt.exception.TokenException
import io.stereov.singularity.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.token.UserTokenService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * # Filter for handling cookie-based authentication.
 *
 * This filter intercepts incoming requests and checks for the presence of an authentication token in the cookies.
 * If a valid token is found, it retrieves the associated user and sets the security context.
 * If the token is invalid or the user is not found, it sets the response status to UNAUTHORIZED.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class CookieAuthenticationFilter(
    private val userTokenService: UserTokenService,
    private val userService: UserService,
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain) = mono {

        val authToken = extractTokenFromRequest(exchange)

        if (!authToken.isNullOrBlank()) {
            val accessToken = try {
                userTokenService.validateAndExtractAccessToken(authToken)
            } catch(e: TokenException) {
                return@mono setSecurityContext(chain, exchange, e)
            }

            val user = try {
                userService.findById(accessToken.userId)
            } catch (e: DocumentNotFoundException) {
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

        chain.filter(exchange).awaitFirstOrNull()
    }

    private fun extractTokenFromRequest(exchange: ServerWebExchange): String? {
        return exchange.request.cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
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
