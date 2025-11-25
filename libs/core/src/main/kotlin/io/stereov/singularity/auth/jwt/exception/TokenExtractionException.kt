package io.stereov.singularity.auth.jwt.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that occur during the process of extracting tokens, such as JSON Web Tokens (JWT).
 *
 * This sealed class serves as the base class for more specific token extraction exceptions,
 * providing context for various failure scenarios. Each subclass defines detailed information
 * about specific exceptions encountered during the token extraction process. The class extends
 * [SingularityException] to include an error message, error code, HTTP status, a description,
 * and an optional cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the error type.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the error.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class TokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating an invalid token during the token extraction process.
     *
     * This exception is a subclass of [TokenExtractionException].
     *
     * @param msg The error message describing the invalid token.
     * @param cause The underlying exception that caused the invalid token error, if any.
     *
     * @property code `INVALID_TOKEN`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class Invalid(msg: String, cause: Throwable? = null) : TokenExtractionException(
        msg,
        "INVALID_TOKEN",
        HttpStatus.UNAUTHORIZED,
        "Represents an exception indicating an invalid token during the token extraction process.",
        cause
    )

    /**
     * Represents an exception indicating that a token has expired during the token extraction process.
     *
     * This exception is a subclass of [TokenExtractionException].
     *
     * @param msg The error message describing the expired token issue.
     * @param cause The underlying exception that caused the expired token issue, if any.
     *
     * @property code `TOKEN_EXPIRED`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class Expired(msg: String, cause: Throwable? = null) : TokenExtractionException(
        msg,
        "TOKEN_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Represents an exception indicating that a token has expired during the token extraction process.",
        cause
    )

    /**
     * Represents an exception indicating that a token is missing during the token extraction process.
     *
     * This exception is a subclass of [TokenExtractionException].
     *
     * @param msg The error message describing the missing token.
     * @param cause The underlying exception that caused the missing token error, if any.
     *
     * @property code `TOKEN_MISSING`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class Missing(msg: String, cause: Throwable? = null) : TokenExtractionException(
        msg,
        "TOKEN_MISSING",
        HttpStatus.UNAUTHORIZED,
        "Represents an exception indicating that a token is missing during the token extraction process.",
        cause
    )
}