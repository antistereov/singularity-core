package io.stereov.web.filter

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.exception.BaseWebException
import io.stereov.web.properties.RateLimitProperties
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration

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
    private val authenticationService: AuthenticationService,
    private val proxyManager: LettuceBasedProxyManager<String>,
    private val rateLimitProperties: RateLimitProperties,
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        return Mono.defer {
            val clientIp = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"

            val ipBucket = resolveBucket("rate_limit_ip:$clientIp", rateLimitProperties.ipRateLimitMinute, 1)

            ipBucket
                .flatMap { checkBucket(it) }
                .then(
                    mono { authenticationService.getCurrentUserId() }
                        .onErrorResume { Mono.empty() }
                        .flatMap { accountId ->
                            val userBucket = resolveBucket("rate_limit_user:$accountId", rateLimitProperties.accountRateLimitMinute, rateLimitProperties.ipRateLimitRefreshMinute)
                            userBucket.flatMap { checkBucket(it) }
                        }
                )
                .then(chain.filter(exchange))
                .onErrorResume { e ->
                    if (e is TooManyRequestsException) {
                        exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                        exchange.response.setComplete()
                    } else {
                        Mono.error(e)
                    }
                }
        }
    }

    private fun resolveBucket(key: String, capacity: Long, periodMinutes: Long = 1): Mono<Bucket> {
        val configuration = BucketConfiguration.builder()
            .addLimit { limit -> limit.capacity(capacity).refillGreedy(capacity, Duration.ofMinutes(periodMinutes))}
            .build()

        return Mono.defer {
            val bucket = proxyManager.getProxy(key) { configuration }
            Mono.just(bucket)
        }
    }

    private fun checkBucket(bucket: Bucket): Mono<Void> {
        return Mono.defer {
            if (bucket.tryConsume(1)) {
                Mono.empty()
            } else {
                Mono.error(TooManyRequestsException("Rate limit exceeded"))
            }
        }
    }

    class TooManyRequestsException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
}
