package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Exception hierarchy related to failures during the creation of an access token.
 *
 * This sealed class serves as the base for specific exceptions that occur
 * when creating an access token, providing detailed error codes and HTTP
 * statuses to handle various scenarios effectively.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure within access token creation.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the specific failure.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class AccessTokenCreationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a failure during the creation of an access token.
     *
     * Extends [AccessTokenCreationException].
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ACCESS_TOKEN_CREATION_FAILED`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Failed(msg: String, cause: Throwable? = null) : AccessTokenCreationException(
        msg,
        "ACCESS_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when a generic exception occurred during the creation of an access token.",
        cause
    )

    /**
     * Represents a specific failure during the creation of an access token related to caching issues.
     *
     * Extends [AccessTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ACCESS_TOKEN_CACHE_FAILURE`.
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR].
     */
    class Cache(msg: String, cause: Throwable? = null) : AccessTokenCreationException(
        msg,
        "ACCESS_TOKEN_CACHE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an access token could not be created due to an exception in the access token whitelist.",
        cause
    )

    /**
     * Represents an exception that occurs during the encoding process of an access token.
     *
     * Extends [AccessTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ACCESS_TOKEN_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Encoding
     */
    class Encoding(msg: String, cause: Throwable? = null) : AccessTokenCreationException(
        msg,
        "ACCESS_TOKEN_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the encoding process of an access token.",
        cause
    )

    /**
     * Represents an exception that occurs when there is a failure related to the secret required for creating an access token.
     *
     * Extends [AccessTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ACCESS_TOKEN_CACHE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Secret
     */
    class Secret(msg: String, cause: Throwable? = null) : AccessTokenCreationException(
        msg,
        "ACCESS_TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to the secret required for creating an access token.",
        cause
    )

    /**
     * Indicates an invalid principal document associated with an access token creation process.
     *
     * This exception is a specific type of [AccessTokenCreationException] that is thrown when the
     * principal document linked to the access token is deemed invalid. It provides relevant
     * contextual information such as the error message, unique error code, HTTP status, and an
     * optional cause.
     *
     * @param msg The error message describing the invalid principal issue.
     * @param cause The underlying cause of this exception, if available.
     *
     * @property code `ACCESS_TOKEN_INVALID_PRINCIPAL_DOCUMENT_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     * @property description "Indicates that the principal document associated with the access token is invalid."
     */
    class InvalidPrincipal(msg: String, cause: Throwable? = null) : AccessTokenCreationException(
        msg,
        "ACCESS_TOKEN_INVALID_PRINCIPAL_DOCUMENT_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Indicates that the principal document associated with the access token is invalid.",
        cause
    )
}
