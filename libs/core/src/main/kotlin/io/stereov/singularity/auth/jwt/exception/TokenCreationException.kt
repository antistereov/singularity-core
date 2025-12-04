package io.stereov.singularity.auth.jwt.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that occur during the creation of tokens, such as JSON Web Tokens (JWT).
 *
 * This sealed class serves as the base class for more specific token creation exceptions,
 * providing context for various failure scenarios. Each subclass defines detailed information
 * about specific exceptions encountered during the token creation process. The class extends
 * [SingularityException] to include an error message, error code, HTTP status, a description,
 * and an optional cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the error type.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the error.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class TokenCreationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception that occurs during the encoding process of a JSON Web EmailVerificationTokenCreation (JWT).
     *
     * This exception is a subclass of [TokenCreationException].
     *
     * @param msg The error message describing the encoding failure.
     * @param cause The underlying exception that caused the encoding failure, if any.
     *
     * @property code `TOKEN_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Encoding(msg: String, cause: Throwable? = null) : TokenCreationException(
        msg,
        "TOKEN_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the encoding process of a JSON Web EmailVerificationTokenCreation (JWT).",
        cause
    )

    /**
     * Represents an exception that occurs when there is a failure related to the secret
     * required for creating a JSON Web EmailVerificationTokenCreation (JWT).
     *
     * This exception is a subclass of [TokenCreationException].
     *
     * @param msg The error message describing the secret-related failure.
     * @param cause The underlying exception that caused the failure, if any.
     *
     * @property code `TOKEN_SECRET_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Secret(msg: String, cause: Throwable? = null) : TokenCreationException(
        msg,
        "TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to the secret required for creating a JSON Web EmailVerificationTokenCreation (JWT).",
        cause
    )
}