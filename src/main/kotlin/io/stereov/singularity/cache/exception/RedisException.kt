package io.stereov.singularity.cache.exception

import io.stereov.singularity.global.exception.BaseWebException

/**
 * # Exception thrown when there is an error related to Redis operations.
 *
 * This exception is used to indicate that there was an issue with Redis operations,
 * such as connection errors, command failures, etc.
 *
 * It extends the [BaseWebException] class.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
open class RedisException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
