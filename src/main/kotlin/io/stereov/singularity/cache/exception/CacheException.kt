package io.stereov.singularity.cache.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a base class for various cache-related exceptions.
 *
 * This sealed class extends [SingularityException] and serves as the root for exceptions
 * that may arise when interacting with cache operations, such as object mapping errors,
 * operational failures, and missing keys. Each subclass of this exception specifies a unique
 * scenario, accompanied by a predefined error code.
 *
 * @constructor Creates an instance of the cache exception with a specified message, error code,
 * and optional underlying cause.
 *
 * @param msg The error message describing the exception.
 * @param code The error code representing the specific exception type.
 * @param cause An optional [Throwable] representing the underlying reason for the exception.
 */
sealed class CacheException(
    msg: String,
    code: String,
    status: HttpStatus,
    cause: Throwable?
) : SingularityException(msg, code, status, cause) {

    /**
     * Represents an exception that occurs during object mapping operations within a cache.
     *
     * @constructor Creates an instance of the exception with a given error message
     * and an optional underlying cause.
     *
     * @param msg The error message describing the failure.
     * @param cause An optional [Throwable] that represents the underlying reason for the exception.
     */
    class ObjectMapper(msg: String, cause: Throwable? = null) : CacheException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "CACHE_OBJECT_MAPPING_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    /**
     * Represents an exception that occurs during cache operation failures.
     *
     * @constructor Creates an instance of the exception with a specified error message
     * and an optional underlying cause.
     *
     * @param msg The error message describing the failure.
     * @param cause An optional [Throwable] representing the underlying reason for the exception.
     */
    class Operation(msg: String, cause: Throwable? = null) : CacheException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "CACHE_OPERATION_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    /**
     * Represents an exception that occurs when a requested key is not found in the cache.
     *
     * @constructor Creates an instance of the exception with the specified error message
     * and an optional underlying cause.
     *
     * @param msg The error message describing the missing key.
     * @param cause An optional [Throwable] that represents the underlying reason for the exception.
     */
    class KeyNotFound(msg: String, cause: Throwable? = null) : CacheException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "CACHE_KEY_NOT_FOUND"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
