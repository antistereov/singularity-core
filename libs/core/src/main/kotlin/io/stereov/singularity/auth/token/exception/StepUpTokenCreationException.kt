package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Encapsulates exceptions that may occur during the creation of step-up tokens.
 *
 * This sealed class extends [SingularityException] and serves as the base class for handling
 * specific errors related to step-up token creation. It provides contextual information such
 * as an error message, unique error code, HTTP status, a detailed description of the problem,
 * and an optional underlying cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the specific failure.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class StepUpTokenCreationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that a step-up token request was made from an unauthorized or illegal context.
     *
     * Extends [StepUpTokenCreationException] and is used to signify permission issues
     * when attempting to create a step-up token.
     *
     * @param msg The error message describing the forbidden action.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `STEP_UP_TOKEN_CREATION_FORBIDDEN`
     * @property status [HttpStatus.FORBIDDEN]
     */
    class Forbidden(msg: String, cause: Throwable? = null) : StepUpTokenCreationException(
        msg,
        "STEP_UP_TOKEN_CREATION_FORBIDDEN",
        HttpStatus.FORBIDDEN,
        "Thrown when a step-up token is requested from an illegal context.",
        cause
    )

    /**
     * Represents an exception that occurs during the encoding process of a step-up token.
     *
     * This exception is a specialization of [StepUpTokenCreationException] with additional metadata,
     * including an error message, unique error code, HTTP status, and optional root cause.
     *
     * @param msg The error message describing the nature of the encoding failure.
     * @param cause The underlying cause of this exception, if available.
     *
     * @property code `STEP_UP_TOKEN_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Encoding
     */
    class Encoding(msg: String, cause: Throwable? = null) : StepUpTokenCreationException(
        msg,
        "STEP_UP_TOKEN_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the encoding process of a step-up token.",
        cause
    )

    /**
     * Represents an exception that occurs when there is a failure related to the secret required for creating a refresh token.
     *
     * This exception is a specialization of [RefreshTokenCreationException] and is used to handle errors specific to issues with
     * the secret during the process of refresh token creation. It encapsulates relevant metadata such as an error message,
     * unique error code, HTTP status, and an optional root cause.
     *
     * @param msg The error message describing the nature of the secret-related failure.
     * @param cause The underlying cause of this exception, if available.
     *
     * @property code `STEP_UP_TOKEN_SECRET_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Secret
     */
    class Secret(msg: String, cause: Throwable? = null) : StepUpTokenCreationException(
        msg,
        "STEP_UP_TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to the secret required for creating a refresh token.",
        cause
    )
}