package io.stereov.singularity.security.ratelimit.service

import com.github.michaelbull.result.getOrThrow
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.ratelimit.excpetion.RateLimitException
import io.stereov.singularity.security.ratelimit.properties.LoginAttemptLimitProperties
import io.stereov.singularity.security.ratelimit.properties.RateLimitProperties
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Service for managing rate limiting functionality.
 *
 * This service provides methods to enforce rate-limiting policies for client IP addresses
 * and authenticated users. It leverages token buckets to manage and track rate limits, ensuring
 * the defined thresholds are not exceeded. Includes support for both general rate limits
 * and login-specific limits by IP address.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class RateLimitService(
    private val authorizationService: AuthorizationService,
    private val proxyManager: LettuceBasedProxyManager<String>,
    private val rateLimitProperties: RateLimitProperties,
    private val loginAttemptLimitProperties: LoginAttemptLimitProperties,
) {
    /**
     * Checks if the client IP address has exceeded the configured rate limit.
     *
     * This method applies rate limiting based on the specified IP address, using a bucket strategy
     * to track and enforce limits. It resolves a rate-limiting bucket for the given IP address and
     * checks whether the usage is within the allowed threshold. If the limit is exceeded, an error
     * is returned.
     *
     * @param clientIp the IP address of the client to be checked for rate limiting
     * @return a Mono signaling completion if the rate limit is not exceeded,
     *         or an error if the client has exceeded the rate limit
     */
    fun checkIpRateLimit(clientIp: String): Mono<Void> {
        return resolveBucket("rate_limit_ip:$clientIp", rateLimitProperties.ipLimit, 1)
            .flatMap { checkBucket(it) { RateLimitException.Ip("Too many login attempts for current IP") } }
    }

    /**
     * Checks if the current user exceeds the configured rate limit.
     *
     * The method retrieves the ID of the currently authenticated user, resolves a rate-limiting bucket specific
     * to that user, and verifies if the user's request consumption is within the allowed limit. If the rate limit
     * is exceeded, an error is emitted; otherwise, the method completes successfully.
     *
     * @return a Mono<Void> that completes if the user is within the rate limit or emits an error if the limit is exceeded
     */
    fun checkUserRateLimit(): Mono<Void?> = mono {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow()
            .requireAuthentication()
            .getOrThrow()
            .userId
    }
        .onErrorResume { Mono.empty() }
        .flatMap { userId ->
            val userBucket = resolveBucket(
                "rate_limit_user:$userId",
                rateLimitProperties.userLimit,
                rateLimitProperties.userTimeWindow
            )
            userBucket.flatMap { checkBucket(it) { RateLimitException.User("Too many requests for current user") } }
        }

    /**
     * Checks if the login attempts from a specific IP address exceed the configured limit
     * and applies rate limiting if necessary.
     *
     * @param clientIp the IP address of the client attempting to log in
     * @return a Mono<Void> indicating the completion of the rate limit check or an error
     *         if the limit is exceeded
     */
    fun checkIpLoginLimit(
        clientIp: String,
    ): Mono<Void> {
        return resolveBucket(
            "login_attempt_ip:$clientIp",
            loginAttemptLimitProperties.ipLimit,
            loginAttemptLimitProperties.ipTimeWindow
        )
            .flatMap { checkBucket(it) { RateLimitException.Login("Too many requests from current IP address") } }
    }

    /**
     * Resolves a bucket that applies rate-limiting rules based on the given key, capacity, and time window.
     *
     * @param key The unique identifier for the rate limit bucket.
     * @param capacity The maximum number of tokens allowed in the bucket.
     * @param periodMinutes The time window in minutes for refilling the bucket. Defaults to 1 minute.
     * @return A Mono emitting the resolved bucket associated with the specified key and configuration.
     */
    fun resolveBucket(key: String, capacity: Long, periodMinutes: Long): Mono<Bucket> {
        val configuration = BucketConfiguration.builder()
            .addLimit { limit -> limit.capacity(capacity).refillGreedy(capacity, Duration.ofMinutes(periodMinutes))}
            .build()

        return Mono.defer {
            val bucket = proxyManager.getProxy(key) { configuration }
            Mono.just(bucket)
        }
    }

    /**
     * Checks if a token can be consumed from the provided `Bucket`. This method enforces
     * the rate-limiting policy by attempting to consume a single token from the bucket.
     *
     * If the bucket allows consumption, the method completes successfully. Otherwise,
     * it emits a `TooManyRequestsException` if the rate limit has been exceeded.
     *
     * @param bucket The `Bucket` instance representing the rate-limit token bucket configuration.
     * @return A `Mono<Void>` that completes if the token was consumed successfully,
     *         or emits an error of type `TooManyRequestsException` if the rate limit is exceeded.
     */
    fun checkBucket(bucket: Bucket, exception: () -> RateLimitException): Mono<Void> {
        return Mono.defer {
            if (bucket.tryConsume(1)) {
                Mono.empty()
            } else {
                Mono.error(exception())
            }
        }
    }
}
