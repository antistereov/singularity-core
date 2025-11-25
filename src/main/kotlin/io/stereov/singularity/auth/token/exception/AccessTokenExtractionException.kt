package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.token.model.AccessToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents the base class for exceptions related to access token extraction failures.
 *
 * This sealed class provides a structured way to handle different scenarios
 * in which access token extraction can fail by offering concrete subclasses
 * with specific error codes, HTTP statuses, and descriptions.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of the failure.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the specific failure.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class AccessTokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the [AccessToken] cannot be decoded.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The cause
     *
     * @property code The error code `ACCESS_TOKEN_INVALID`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Invalid
     */
    class Invalid(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(
        msg,
        "ACCESS_TOKEN_INVALID",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the access token cannot be decoded.",
        cause
    )

    /**
     * Indicates that the [AccessToken] is expired.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The cause
     *
     * @property code The error code `ACCESS_TOKEN_EXPIRED`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Expired
     */
    class Expired(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(
        msg,
        "ACCESS_TOKEN_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the access token is expired.",
        cause
    )

    /**
     * Represents an exception indicating a failure when reading the allowlist for an access token cache.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ACCESS_TOKEN_ALLOWLIST_READING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Cache(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(
        msg,
        "ACCESS_TOKEN_ALLOWLIST_READING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception indicating a failure when reading the allowlist for an access token cache.",
        cause
    )

}
