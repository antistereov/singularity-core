package io.stereov.singularity.database.encryption.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specifically related to encryption operations.
 *
 * This is a sealed class extending [SingularityException] and serves as the base class for all exceptions
 * related to encryption processes within the application.
 *
 * @param msg The error message describing the nature of the exception.
 * @param code The unique error code associated with this exception.
 * @param status The HTTP status corresponding to the exception.
 * @param description A detailed description providing additional context about the exception.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class EncryptionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing a failure related to object mapping during encryption operations.
     *
     * This exception is a specific type of [EncryptionException].
     *
     * @param msg The error message describing the object mapping failure.
     * @param cause The underlying cause of this exception, if available.
     *
     * @property code `ENCRYPTION_OBJECT_MAPPING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class ObjectMapping(msg: String, cause: Throwable? = null) : EncryptionException(
        msg,
        "ENCRYPTION_OBJECT_MAPPING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a failure related to object mapping during encryption operations.",
        cause
    )

    /**
     * Exception representing a failure related to encryption or decryption operations.
     *
     * This class extends [EncryptionException].
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ENCRYPTION_CIPHER_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Cipher(msg: String, cause: Throwable? = null) : EncryptionException(
        msg,
        "ENCRYPTION_CIPHER_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a failure related to encryption or decryption operations.",
        cause
    )

    /**
     * Represents an exception indicating a failure related to encryption secrets.
     *
     * This exception is a specific type of [EncryptionException].
     *
     * @param msg The error message describing the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ENCRYPTION_SECRET_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Secret(msg: String, cause: Throwable? = null) : EncryptionException(
        msg,
        "ENCRYPTION_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception indicating a failure related to encryption secrets.",
        cause
    )

    /**
     * Exception representing an encoding failure during encryption operations.
     *
     * This exception is a specific type of [EncryptionException].
     *
     * @param msg The error message describing the encoding failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ENCRYPTION_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Encoding(msg: String, cause: Throwable? = null) : EncryptionException(
        msg,
        "ENCRYPTION_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing an encoding failure during encryption operations.",
        cause
    )
}