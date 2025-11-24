package io.stereov.singularity.file.core.exception

import io.stereov.singularity.file.core.exception.FileException.MetadataOutOfSync.Companion.CODE
import io.stereov.singularity.file.core.exception.FileException.MetadataOutOfSync.Companion.STATUS
import io.stereov.singularity.file.core.exception.FileException.Operation.Companion.CODE
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents the base exception class for all file-related errors.
 *
 * This sealed class extends [SingularityException] and serves as the foundation for various
 * specific exceptions related to file operations.
 * Each subclass within [FileException] is tailored to specific error scenarios,
 * with unique error codes for categorization.
 *
 * @param msg The error message describing the context or nature of the exception.
 * @param code The predefined error code associated with the specific exception type.
 * @param cause The root cause of the exception, if applicable.
 */
sealed class FileException(msg: String, code: String, status: HttpStatus, cause: Throwable? = null) : SingularityException(msg, code, status, cause) {

    /**
     * Thrown to indicate that an operation failed due to an unsupported media type.
     *
     * This exception is specifically used to represent scenarios where a provided
     * media type is not compatible or allowed in the current context of file operations.
     * It carries a predefined error code, "UNSUPPORTED_MEDIA_TYPE", that can be used
     * for error tracking and handling.
     *
     * @param msg A descriptive message providing details about the exception.
     * @param cause The underlying cause of the exception, if available.
     */
    class UnsupportedMediaType(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "UNSUPPORTED_MEDIA_TYPE"
            val STATUS = HttpStatus.UNSUPPORTED_MEDIA_TYPE
        }
    }

    /**
     * Thrown to indicate that a failure occurred during the handling of file metadata.
     *
     * This exception is specifically used to represent scenarios where an operation
     * involving file metadata cannot be completed successfully due to an error. It
     * contains a predefined error code, "FILE_METADATA_FAILURE", which can be used
     * for tracking and categorizing exceptions to this type.
     *
     * @constructor Creates a `Metadata` exception instance with the specified message and optional cause.
     * @param msg A message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     */
    class Metadata(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "FILE_METADATA_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    /**
     * Represents an exception that indicates a file key conflict has occurred.
     *
     * This exception is thrown when attempting to use a file key that is already taken or in use.
     *
     * @constructor Creates a [FileKeyTaken] exception with the specified message, error code,
     * and optional cause for further diagnosis.
     * @param msg The detail message regarding the exception.
     * @param cause The cause of this exception, or `null` if no cause is specified.
     */
    class FileKeyTaken(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "FILE_KEY_TAKEN"
            val STATUS = HttpStatus.CONFLICT
        }
    }

    /**
     * Represents an exception thrown when file metadata saved in the database is out of sync
     * with the expected state of the actual file.
     *
     * @param msg The detail message explaining the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property CODE The error code `FILE_METADATA_OUT_OF_SYNC`
     * @property STATUS The status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class MetadataOutOfSync(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "FILE_METADATA_OUT_OF_SYNC"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    /**
     * Represents an exception specifically for file operation failures.
     *
     * This exception is a subclass of [FileException] and is used to handle
     * cases where an operation on a file encounters an error. It includes
     * a default error code [CODE] that signifies a file operation failure.
     *
     * @constructor Creates an instance of the Operation exception.
     * @param msg The error message describing the file operation failure.
     * @param cause The throwable that caused this exception, if any.
     */
    class Operation(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "FILE_OPERATION_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    class Stream(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "FILE_STREAM_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    class NotFound(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "FILE_NOT_FOUND"
            val STATUS = HttpStatus.NOT_FOUND
        }
    }

    class BadRequest(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "BAD_FILE_REQUEST"
            val STATUS = HttpStatus.BAD_REQUEST
        }
    }

    class NotAuthorized(msg: String, cause: Throwable? = null) : FileException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "FILE_ACCESS_NOT_AUTHORIZED"
            val STATUS = HttpStatus.FORBIDDEN
        }
    }
}
