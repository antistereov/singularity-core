package io.stereov.web.filter

import io.stereov.web.global.service.ratelimit.RateLimitService
import io.stereov.web.global.service.ratelimit.excpetion.model.TooManyRequestsException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * # Filter for rate limiting incoming requests.
 *
 * This filter limits the number of requests from a single IP address and user account
 * within a specified time period.
 *
 * It uses the Bucket4j library to manage the rate limiting.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class RateLimitingFilter(
    private val rateLimitService: RateLimitService
) : WebFilter {

    /**
     * Filters incoming web requests to enforce rate limiting based on IP address and account-specific limits.
     * It checks for IP-based, user-based, and login-specific rate limits to prevent abuse.
     *
     * @param exchange the current server web exchange containing the HTTP request and response
     * @param chain the web filter chain allowing further processing of the request
     * @return a Mono<Void> indicating the completion of the filter processing or an error if rate limits are exceeded
     */
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return Mono.defer {
            val clientIp = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
            val path = exchange.request.path.toString()
            val isLoginAttempt = path.contains("/user/login") ||
                    path.contains("/user/2fa/recovery") ||
                    path.contains("/user/mail/reset-password") ||
                    path.contains("/user/2fa/verify-step-up") ||
                    path.contains("/user/2fa/verify-login")

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
