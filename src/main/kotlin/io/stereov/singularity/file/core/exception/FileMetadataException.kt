package io.stereov.singularity.file.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specific to file metadata operations.
 *
 * This sealed class serves as the base for all file-metadata-related exceptions.
 * It extends [SingularityException] and provides a common structure,
 * including a message, error code, HTTP status, and description for such failures.
 *
 * @param msg The error message describing the issue.
 * @param code The specific error code associated with this exception.
 * @param status The HTTP status that corresponds to this exception.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class FileMetadataException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the requested file metadata could not be found.
     *
     * This exception is a subclass of [FileMetadataException].
     *
     * @param msg A descriptive message detailing the context of the error.
     * @param cause The underlying cause of this exception, if available.
     *
     * @property code `FILE_METADATA_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : FileMetadataException(
        msg,
        "FILE_METADATA_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Indicates that the requested file metadata could not be found.",
        cause
    )

    /**
     * Represents an exception related to operations with file metadata arising from database-related issues.
     *
     * This exception is a subclass of [FileMetadataException].
     *
     * @param msg A detailed message describing the error.
     * @param cause The root cause of this exception, if available.
     *
     * @property code `DATABASE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : FileMetadataException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception related to operations with file metadata arising from database-related issues.",
        cause
    )
}
