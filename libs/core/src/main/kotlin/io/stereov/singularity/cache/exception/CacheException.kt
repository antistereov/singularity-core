package io.stereov.singularity.cache.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a base exception specific to cache-related operations.
 *
 * This is a sealed class that extends [SingularityException] and acts as a parent
 * for more specific cache-related exceptions. It provides additional context such
 * as an error message, a specific error code, an HTTP status, and an optional
 * description to facilitate better error handling and debugging.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code identifying the type of cache error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the cache error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class CacheException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception that occurs during object mapping operations within a cache.
     *
     * Extends [CacheException].
     *
     * @param msg The error message describing the failure.
     * @param cause An optional [Throwable] that represents the underlying reason for the exception.
     *
     * @property code `CACHE_OBJECT_MAPPING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class ObjectMapper(msg: String, cause: Throwable? = null) : CacheException(
        msg,
        "CACHE_OBJECT_MAPPING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during object mapping operations within a cache.",
        cause
    )

    /**
     * Represents an exception that occurs during cache operation failures.
     *
     * Extends [CacheException].
     *
     * @param msg The error message describing the failure.
     * @param cause An optional [Throwable] representing the underlying reason for the exception.
     *
     * @property code `CACHE_OPERATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Operation(msg: String, cause: Throwable? = null) : CacheException(
        msg,
        "CACHE_OPERATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during cache operation failures.",
        cause
    )

    /**
     * Represents an exception that occurs when a requested key is not found in the cache.
     *
     * @param msg The error message describing the missing key.
     * @param cause An optional [Throwable] that represents the underlying reason for the exception.
     *
     * @property code `CACHE_KEY_NOT_FOUND`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class KeyNotFound(msg: String, cause: Throwable? = null) : CacheException(
        msg,
        "CACHE_KEY_NOT_FOUND",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when a requested key is not found in the cache.",
        cause
    )
}
