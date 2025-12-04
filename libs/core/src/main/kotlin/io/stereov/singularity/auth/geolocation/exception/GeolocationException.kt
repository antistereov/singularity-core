package io.stereov.singularity.auth.geolocation.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents an exception related to errors occurring during the download of the geolocation database.
 *
 * This sealed class is a specialized form of [SingularityException] and serves as the base class
 * for all exceptions specific to geolocation database download errors. It provides detailed context
 * such as an error message, unique error code, HTTP status, a description of the error, and an
 * optional underlying cause.
 *
 * @param message The error message describing the exception.
 * @param code A unique code representing the specific download error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class GeolocationException(
    message: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(message, code, status, description, cause) {

    /**
     * An exception thrown when the retrieval of geolocation data fails.
     *
     * Extends [GeolocationException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GEOLOCATION_RETRIEVAL_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Get(msg: String, cause: Throwable? = null) : GeolocationException(
        msg,
        "GEOLOCATION_RETRIEVAL_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when it fails to retrieve the geolocation.",
        cause
    )

    /**
     * Exception thrown when a generic error occurs while attempting to download the geolocation database.
     *
     * This is a specialized form of [GeolocationException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GEOLOCATION_DB_DOWNLOAD_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : GeolocationException(
        msg,
        "GEOLOCATION_DB_DOWNLOAD_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when a generic exception occurs when trying to download the geolocation database.",
        cause
    )

    /**
     * Exception thrown when the initialization of the geolocation database fails.
     *
     * This is a specialized form of [GeolocationException] used to encapsulate errors
     * occurring during the initialization process of the geolocation database.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GEOLOCATION_DB_INIT_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Init(msg: String, cause: Throwable? = null) : GeolocationException(
        msg,
        "GEOLOCATION_DB_INIT_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when the initialization of the geolocation DB failed.",
        cause
    )

    /**
     * This exception is thrown when an error occurs while attempting to save the downloaded geolocation database.
     *
     * @param msg Descriptive error message providing details about the failure.
     * @param cause The underlying throwable cause of this exception, if available.
     *
     * @property code `GEOLOCATION_DB_SAVE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Save(msg: String, cause: Throwable? = null) : GeolocationException(
        msg,
        "GEOLOCATION_DB_SAVE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an error occurs when trying to save the downloaded geolocation db.",
        cause
    )

    /**
     * This exception is thrown when the geolocation database download request limit is exceeded.
     *
     * It provides information about the error using the message, a specific error code, HTTP status,
     * and an optional underlying cause.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `TOO_MANY_GEOLOCATION_DOWNLOADS_REQUESTS`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class TooManyRequests(msg: String, cause: Throwable? = null) : GeolocationException(
        msg,
        "TOO_MANY_GEOLOCATION_DB_DOWNLOADS_REQUESTS",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when the geolocation database could not be downloaded due to too many requests.",
        cause
    )

    /**
     * Exception thrown when there is an authentication failure during the download of the geolocation database.
     *
     * This is a specialized form of [GeolocationException]. It provides detailed context about the error,
     * including the error message, a unique error code, the associated HTTP status, a description indicating the
     * nature of the exception, and an optional underlying cause of the issue.
     *
     * @param msg The error message providing details about the authentication failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GEOLOCATION_DB_AUTHENTICATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Authentication(msg: String, cause: Throwable? = null) : GeolocationException(
        msg,
        "GEOLOCATION_DB_AUTHENTICATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when the geolocation database cannot be downloaded due to an authentication failure",
        cause
    )
}