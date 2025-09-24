package io.stereov.singularity.ratelimit.filter

import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.global.util.getClientIp
import io.stereov.singularity.ratelimit.excpetion.model.TooManyRequestsException
import io.stereov.singularity.ratelimit.service.RateLimitService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * # Filter for rate limiting incoming requests.
 *
 * This ratelimit limits the number of requests from a single IP address and user account
 * within a specified time period.
 *
 * It uses the Bucket4j library to manage the rate limiting.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class RateLimitFilter(
    private val rateLimitService: RateLimitService,
    private val geolocationProperties: GeolocationProperties
) : WebFilter {

    /**
     * Filters incoming web requests to enforce rate limiting based on IP address and account-specific limits.
     * It checks for IP-based, user-based, and login-specific rate limits to prevent abuse.
     *
     * @param exchange the current server web exchange containing the HTTP request and response
     * @param chain the web ratelimit chain allowing further processing of the request
     * @return a Mono<Void> indicating the completion of the ratelimit processing or an error if rate limits are exceeded
     */
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return Mono.defer {
            val clientIp = exchange.request.getClientIp(geolocationProperties.realIpHeader) ?: "unknown"
            val path = exchange.request.path.toString()
            val isLoginAttempt = path.contains("/api/auth/login") ||
                    path.contains("/api/auth/2fa/recovery") ||
                    path.contains("/api/auth/password/reset") ||
                    path.contains("/api/auth/2fa/step-up") ||
                    path.contains("/api/auth/2fa/login")

            rateLimitService.checkIpRateLimit(clientIp)
                .then(rateLimitService.checkUserRateLimit())
                .then(if (isLoginAttempt) rateLimitService.checkIpLoginLimit(clientIp) else Mono.empty())
                .then(chain.filter(exchange))
                .onErrorResume { e ->
                    if (e is TooManyRequestsException) {
                        exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                        exchange.response.setComplete()
                    } else { Mono.error(e) }
                }
        }
    }
}
