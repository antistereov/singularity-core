package io.stereov.web.filter

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.exception.BaseWebException
import io.stereov.web.properties.LoginAttemptLimitProperties
import io.stereov.web.properties.RateLimitProperties
import io.stereov.web.user.dto.request.LoginRequest
import kotlinx.coroutines.reactor.mono
import org.springframework.core.io.buffer.DataBufferUtils
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
    private val loginAttemptLimitProperties: LoginAttemptLimitProperties,
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        return Mono.defer {
            val clientIp = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
            val isLoginAttempt = exchange.request.path.toString().contains("/user/login")

            val ipBucket = resolveBucket("rate_limit_ip:$clientIp", rateLimitProperties.ipLimit, 1)

            val objectMapper = ObjectMapper()

            ipBucket
                .flatMap { checkBucket(it) }
                .then(
                    if (isLoginAttempt) {
                        val failedLoginBucket = resolveBucket("login_attempt_ip:$clientIp", loginAttemptLimitProperties.ipLimit, loginAttemptLimitProperties.ipTimeWindowMinutes)
                        failedLoginBucket.flatMap { checkBucket(it) }
                    } else {
                        Mono.empty()
                    }
                )
                .then(
                    mono { authenticationService.getCurrentUserId() }
                        .onErrorResume { Mono.empty() }
                        .flatMap { userId ->
                            val userBucket = resolveBucket("rate_limit_user:$userId", rateLimitProperties.userLimit, rateLimitProperties.userTimeWindowMinutes)
                            userBucket.flatMap { checkBucket(it) }
                        }
                )
                .then(
                    if (isLoginAttempt) {
                        exchange.request.body
                            .transform { body ->
                                DataBufferUtils.join(body)
                                    .map { dataBuffer ->
                                        val bytes = ByteArray(dataBuffer.readableByteCount())
                                        dataBuffer.read(bytes)
                                        DataBufferUtils.release(dataBuffer)

                                        objectMapper.readValue(bytes, LoginRequest::class.java)
                                    }
                            }
                            .collectList()
                            .map { it.first() }
                            .flatMap { loginRequest ->
                                val email = loginRequest.email
                                val failedLoginBucket = resolveBucket("login_attempt_email:$email", loginAttemptLimitProperties.emailLimit, loginAttemptLimitProperties.emailTimeWindowMinutes)
                                failedLoginBucket.flatMap { checkBucket(it) }
                            }
                    } else {
                        Mono.empty()
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
