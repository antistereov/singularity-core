package io.stereov.singularity.core.global.service.ratelimit.excpetion

import io.stereov.singularity.core.global.exception.BaseWebException

/**
 * Exception thrown when a rate limit has been exceeded.
 *
 * Extends the [BaseWebException] to provide additional context for rate-limiting errors.
 * Designed to be used in scenarios where requests surpass the allowed limit.
 *
 * @param message The error message providing details about the rate limit breach.
 * @param cause The underlying cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
open class RateLimitException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
