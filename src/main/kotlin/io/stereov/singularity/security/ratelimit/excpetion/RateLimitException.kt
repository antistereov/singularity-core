package io.stereov.singularity.ratelimit.excpetion

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents an exception indicating that a rate limit has been exceeded.
 *
 * This is a sealed class serving as a base for more specific rate limit exceptions. It provides
 * relevant information like the error code, HTTP status, and a detailed description to assist
 * in identifying and handling different rate-limiting scenarios.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the specific rate limit exceeded.
 * @param status The associated HTTP status for the exception, typically [HttpStatus.TOO_MANY_REQUESTS].
 * @param description A detailed description providing more context about the rate limit violation.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class RateLimitException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception thrown when the rate limit for requests from a single IP address is exceeded.
     *
     * This exception is a specific subclass of [RateLimitException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `IP_RATE_LIMIT_EXCEEDED`
     * @property status [HttpStatus.TOO_MANY_REQUESTS]
     */
    class Ip(msg: String, cause: Throwable? = null) : RateLimitException(
        msg,
        "IP_RATE_LIMIT_EXCEEDED",
        HttpStatus.TOO_MANY_REQUESTS,
        "Too many requests from a single IP address.",
        cause
    )

    /**
     * Exception thrown when the rate limit for requests from a single user is exceeded.
     *
     * This exception is a specific subclass of [RateLimitException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `USER_RATE_LIMIT_EXCEEDED`
     * @property status [HttpStatus.TOO_MANY_REQUESTS]
     */
    class User(msg: String, cause: Throwable? = null) : RateLimitException(
        msg,
        "USER_RATE_LIMIT_EXCEEDED",
        HttpStatus.TOO_MANY_REQUESTS,
        "Too many requests from a single user.",
        cause
    )

    /**
     * Exception thrown when the rate limit for login attempts from a single IP address is exceeded.
     *
     * This exception is a specific subclass of [RateLimitException] and indicates that
     * the number of login attempts from a single IP has exceeded the configured limit.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `IP_LOGIN_RATE_LIMIT_EXCEEDED`
     * @property status [HttpStatus.TOO_MANY_REQUESTS]
     */
    class Login(msg: String, cause: Throwable? = null) : RateLimitException(
        msg,
        "IP_LOGIN_RATE_LIMIT_EXCEEDED",
        HttpStatus.TOO_MANY_REQUESTS,
        "Too many login attempts from a single IP address.",
        cause
    )
}
