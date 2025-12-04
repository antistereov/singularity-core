package io.stereov.singularity.secrets.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents an exception that occurs during the secret rotation process.
 *
 * This sealed class serves as the base for specific exceptions connected to the
 * secret rotation process, providing additional context such as error codes,
 * HTTP status, and detailed descriptions of the issue. It extends the [SingularityException]
 * class to integrate with the application's error-handling structure.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description offering more insight about the error.
 * @param cause The underlying cause of the exception, if applicable.
 */
sealed class SecretRotationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating that a secret rotation process is ongoing.
     *
     * This exception extends [SecretRotationException].
     *
     * @param msg The error message providing details about the ongoing rotation.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `SECRET_ROTATION_ONGOING`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class Ongoing(msg: String, cause: Throwable? = null) : SecretRotationException(
        msg,
        "SECRET_ROTATION_ONGOING",
        HttpStatus.BAD_REQUEST,
        "Represents an exception indicating that a secret rotation process is ongoing.",
        cause
    )
}
