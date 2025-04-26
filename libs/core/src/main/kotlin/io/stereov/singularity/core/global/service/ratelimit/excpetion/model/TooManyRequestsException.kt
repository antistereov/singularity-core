package io.stereov.singularity.core.global.service.ratelimit.excpetion.model

import io.stereov.singularity.core.global.service.ratelimit.excpetion.RateLimitException

/**
 * Exception thrown when a client exceeds the allowed number of requests.
 *
 * Extends the [RateLimitException] to provide specific context for HTTP 429 (Too Many Requests) scenarios.
 * Commonly used in web service applications to indicate rate-limiting enforcement when a client surpasses their quota.
 *
 * @param message A description of the exceeded rate limit.
 * @param cause The underlying cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class TooManyRequestsException(message: String, cause: Throwable? = null) : RateLimitException(message, cause)
