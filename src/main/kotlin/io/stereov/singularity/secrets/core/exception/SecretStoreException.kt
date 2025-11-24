package io.stereov.singularity.secrets.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a base exception for errors occurring within the secret store.
 *
 * This sealed class serves as a foundation for specific secret store exceptions, providing
 * additional context with error codes, HTTP status, and detailed descriptions. It extends
 * the [SingularityException] class to integrate with the application's error-handling framework.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class SecretStoreException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception thrown when a requested secret cannot be found in the secret store.
     *
     * Extends [SecretStoreException]
     *
     * @param msg The error message describing the missing secret.
     * @param cause The cause of the exception, if any.
     *
     * @property code `SECRET_NOT_FOUND`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class NotFound(msg: String, cause: Throwable? = null) : SecretStoreException(
        msg,
        "SECRET_NOT_FOUND",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception thrown when a requested secret cannot be found in the secret store.",
        cause
    )

    /**
     * Exception representing the failure of cryptographic key generation within the secret store operations.
     *
     * Extends [SecretStoreException].
     *
     * @param msg The error message describing the key generation failure.
     * @param cause The cause of the exception, if any.
     *
     * @property code `SECRET_KEY_GENERATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class KeyGenerator(msg: String, cause: Throwable? = null) : SecretStoreException(
        msg,
        "SECRET_KEY_GENERATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing the failure of cryptographic key generation within the secret store operations.",
        cause
    )

    /**
     * Represents an exception that occurs during a secret store operation.
     *
     * Extends [SecretStoreException].
     *
     * @param msg The detail message for the exception.
     * @param cause The cause of the exception, which can be null.
     *
     * @property code `SECRET_OPERATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Operation(msg: String, cause: Throwable? = null) : SecretStoreException(
        msg,
        "SECRET_OPERATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during a secret store operation.",
        cause
    )
}