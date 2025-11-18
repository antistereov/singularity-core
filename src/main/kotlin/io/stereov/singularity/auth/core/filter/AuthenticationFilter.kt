package io.stereov.singularity.auth.core.filter

import com.github.michaelbull.result.getOrElse
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.core.model.token.AuthenticationFilterExceptionToken
import io.stereov.singularity.auth.core.model.token.AuthenticationToken
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * A web filter responsible for handling authentication in a reactive environment.
 * This filter extracts an access token from the incoming request, validates it,
 * and sets up a security context containing the authenticated user's details.
 * It delegates further request processing to the next filter in the chain.
 *
 * The filter interacts with the following components:
 * - An [AccessTokenService] to handle extraction and validation of access tokens.
 * - The [ReactiveSecurityContextHolder] to store the security context in the reactive context.
 *
 * If an access token cannot be extracted or is invalid, an exception token is created
 * and added to the security context, allowing downstream components to handle the error condition.
 *
 * Primary Responsibilities:
 * - Extract and validate the access token using the [AccessTokenService].
 * - Construct an [AuthenticationToken] upon successful access token validation.
 * - Create and propagate a [SecurityContext] containing authentication details.
 * - Handle token extraction failures by adding an exception token to the security context.
 *
 * This class implements the [WebFilter] interface used in reactive web applications.
 */
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
        exception: AccessTokenExtractionException
    ): Void? {
        val auth = AuthenticationFilterExceptionToken(exception)
        val securityContext = SecurityContextImpl(auth)

        return chain
            .filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
            .awaitFirstOrNull()
    }
}
