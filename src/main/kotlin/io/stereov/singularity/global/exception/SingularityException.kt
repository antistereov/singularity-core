package io.stereov.singularity.global.exception

import org.springframework.http.HttpStatus

/**
 * Represents a custom exception used within the application for handling errors.
 *
 * This exception extends [RuntimeException], adding additional context such as an error
 * code, HTTP status, and a detailed description to provide more information about the error.
 * It serves as a base exception class for more specific exceptions across the application.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
open class SingularityException(
    msg: String,
    val code: String,
    val status: HttpStatus,
    val description: String,
    cause: Throwable? = null,
) : RuntimeException(cause) {
    override val message: String = msg
}
