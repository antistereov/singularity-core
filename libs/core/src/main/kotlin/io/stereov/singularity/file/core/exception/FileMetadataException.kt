package io.stereov.singularity.file.core.exception

import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.global.exception.SingularityException

/**
 * Represents exceptions related to file metadata operations.
 *
 * This sealed class serves as the base for all errors that occur during interactions
 * with file metadata. It extends [SingularityException], inheriting properties for error
 * message, code, and optional underlying cause. Subtypes of this exception specialize
 * in representing specific categories of file metadata-related issues.
 *
 * @param msg The error message describing the issue.
 * @param code The error code identifier.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class FileMetadataException (msg: String, code: String, cause: Throwable? = null) : SingularityException(msg, code, cause) {

    /**
     * Indicates that the requested file metadata could not be found.
     *
     * This exception is thrown when an operation fails due to missing or nonexistent file metadata.
     * It includes a predefined error code, "FILE_METADATA_NOT_FOUND", to facilitate error categorization and tracking.
     *
     * @constructor Creates an instance of the NotFound exception.
     * @param msg A descriptive message detailing the context of the error.
     * @param cause The underlying cause of this exception, if available.
     */
    class NotFound(msg: String, cause: Throwable? = null) : FileMetadataException(msg, CODE, cause) {
        companion object { const val CODE = "FILE_METADATA_NOT_FOUND" }
    }

    /**
     * Represents an exception related to operations with file metadata arising from database-related issues.
     *
     * This exception is a specific subtype of [FileMetadataException] and is used to signify that a file
     * metadata operation failed due to an underlying database problem.
     *
     * @param msg A detailed message describing the error.
     * @param cause The root cause of this exception, if available.
     */
    class Database(msg: String, cause: Throwable? = null) : FileMetadataException(msg, CODE, cause) {
        companion object { const val CODE = DatabaseException.Database.CODE }
    }
}
