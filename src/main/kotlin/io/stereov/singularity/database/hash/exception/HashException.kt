package io.stereov.singularity.database.hash.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a family of exceptions related to hashing operations.
 *
 * This sealed class serves as a base for more specific exceptions that may occur
 * during hashing operations within the application, such as secret handling,
 * encoding, or hashing failures. Each subclass provides a unique context and
 * specific details about the error encountered.
 *
 * Extends [SingularityException].
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the specific type of hashing error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class HashException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception that occurs when there is a failure related to hash secrets.
     *
     * Extends [HashException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `HASH_SECRET_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Secret(msg: String, cause: Throwable? = null): HashException(
        msg,
        "HASH_SECRET_FAILURE", 
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to hash secrets.",
        cause
    )
    /**
     * Represents an exception that occurs during encoding operations in the hashing process.
     *
     * Extends [HashException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `HASH_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Encoding(msg: String, cause: Throwable? = null): HashException(
        msg,
        "HASH_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during encoding operations in the hashing process.",
        cause
    )

    /**
     * Represents an exception that occurs during the hashing process.
     *
     * Extends [HashException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @see HashFailure
     */
    class Hashing(msg: String, cause: Throwable? = null): HashException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )
}