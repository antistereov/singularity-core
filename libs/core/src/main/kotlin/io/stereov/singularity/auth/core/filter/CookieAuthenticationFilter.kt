package io.stereov.singularity.auth.core.filter

import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.model.CustomAuthenticationToken
import io.stereov.singularity.auth.core.model.ErrorAuthenticationToken
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.auth.token.service.AccessTokenService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.apache.http.HttpHeaders
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * # Filter for handling cookie-based authentication.
 *
 * This ratelimit intercepts incoming requests and checks for the presence of an authentication token in the cookies.
 * If a valid token is found, it retrieves the associated user and sets the security context.
 * If the token is invalid or the user is not found, it sets the response status to UNAUTHORIZED.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class CookieAuthenticationFilter(
    private val accessTokenService: AccessTokenService,
    private val userService: UserService,
    private val authProperties: AuthProperties
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain) = mono {

        val authToken = extractTokenFromRequest(exchange)

        if (!authToken.isNullOrBlank()) {
            val accessToken = try {
                accessTokenService.validateAndExtractAccessToken(authToken)
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

        chain.filter(exchange).awaitFirstOrNull()
    }

    private fun extractTokenFromRequest(exchange: ServerWebExchange): String? {
        val cookieToken = exchange.request.cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value

        if (!authProperties.allowHeaderAuthentication) return cookieToken

        val headerToken = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")

        return if (authProperties.preferHeaderAuthentication) {
            headerToken ?: cookieToken
        } else {
            cookieToken ?: headerToken
        }
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
