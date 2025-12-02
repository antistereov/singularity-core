package io.stereov.singularity.file.core.exception

import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.content.core.exception.ContentException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a base class for exceptions related to file operations.
 *
 * This sealed class is designed to handle various file-related exceptions, including file metadata
 * issues, unsupported media types, file operation errors, and others. Each specific file-related
 * exception is implemented as a nested subclass of this base exception.
 *
 * @param msg The detailed message associated with the exception.
 * @param code The unique error code for the specific exception.
 * @param status The associated HTTP status code.
 * @param description A meaningful description of the exception.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class FileException(msg: String, code: String, status: HttpStatus, description: String, cause: Throwable? = null) : SingularityException(msg, code, status, description, cause) {

    /**
     * Thrown to indicate that a file operation failed due to an unsupported media type.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg A descriptive message providing details about the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see UnsupportedMediaTypeFailure
     */
    class UnsupportedMediaType(msg: String, cause: Throwable? = null) : FileException(
        msg,
        UnsupportedMediaTypeFailure.CODE,
        UnsupportedMediaTypeFailure.STATUS,
        UnsupportedMediaTypeFailure.DESCRIPTION,
        cause
    )

    /**
     * Thrown to indicate that a failure occurred during the handling of file metadata which is stored in the database.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg A message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `FILE_METADATA_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Metadata(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "FILE_METADATA_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown to indicate that a failure occurred during the handling of file metadata which is stored in the database.",
        cause
    )

    /**
     * This exception is thrown when attempting to store a file with a file key that is already taken or in use.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg The detail message regarding the exception.
     * @param cause The cause of this exception, or `null` if no cause is specified.
     *
     * @property code `FILE_KEY_TAKEN`
     * @property status [HttpStatus.CONFLICT]
     */
    class FileKeyTaken(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "FILE_KEY_TAKEN",
        HttpStatus.CONFLICT,
        "This exception is thrown when attempting to store a file with a file key that is already taken or in use.",
        cause
    )

    /**
     * Represents an exception thrown when file metadata saved in the database is out of sync
     * with the expected state of the actual file.
     *
     * @param msg The detail message explaining the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code The error code `FILE_METADATA_OUT_OF_SYNC`
     * @property status The status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class MetadataOutOfSync(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "FILE_METADATA_OUT_OF_SYNC",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception thrown when file metadata saved in the database is out of sync " +
                "with the expected state of the actual file.",
        cause
    )

    /**
     * Represents an error that occurred during a file operation, such as saving or updating a file.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg The error message describing the file operation failure.
     * @param cause The throwable that caused this exception, if any.
     *
     * @property code `FILE_OPERATION_FAILURE`
     * @property status[HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Operation(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "FILE_OPERATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an error that occurred during a file operation, such as saving or updating a file.",
        cause
    )

    /**
     * Thrown when an error occurred during a file stream.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg The error message describing the file operation failure.
     * @param cause The throwable that caused this exception, if any.
     *
     * @property code `FILE_STREAM_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Stream(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "FILE_STREAM_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an error occurred during a file stream.",
        cause
    )

    /**
     * Thrown when a file is not found.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg The error message describing the file operation failure.
     * @param cause The throwable that caused this exception, if any.
     *
     * @property code `FILE_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "FILE_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Thrown when the requested file is not found.",
        cause
    )

    /**
     * Thrown when a file request is malformed or invalid.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg The error message describing the file operation failure.
     * @param cause The throwable that caused this exception, if any.
     *
     * @property code `BAD_FILE_REQUEST`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class BadRequest(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "BAD_FILE_REQUEST",
        HttpStatus.BAD_REQUEST,
        "Thrown when the file request is malformed or invalid.",
        cause
    )

    /**
     * Thrown when access to the requested file is not permitted.
     *
     * This exception is a subclass of [FileException].
     *
     * @param msg The error message describing the file operation failure.
     * @param cause The throwable that caused this exception, if any.
     *
     * @property code `FILE_ACCESS_NOT_AUTHORIZED`
     * @property status [HttpStatus.FORBIDDEN]
     */
    class NotAuthorized(msg: String, cause: Throwable? = null) : FileException(
        msg,
        "FILE_ACCESS_NOT_AUTHORIZED",
        HttpStatus.FORBIDDEN,
        "Thrown when access to the requested file is not permitted.",
        cause
    )

    class NotAuthenticated(msg: String, cause: Throwable? = null) : FileException(
        msg,
        AuthenticationException.AuthenticationRequired.CODE,
        AuthenticationException.AuthenticationRequired.STATUS,
        AuthenticationException.AuthenticationRequired.DESCRIPTION,
        cause
    )

    companion object {
        fun from(ex: ContentException) : FileException {
            return when (ex) {
                is ContentException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
                is ContentException.NotAuthenticated -> NotAuthenticated(ex.message, ex.cause)
            }
        }
    }
}
