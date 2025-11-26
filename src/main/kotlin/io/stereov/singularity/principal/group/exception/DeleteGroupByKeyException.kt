package io.stereov.singularity.principal.group.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the deletion of a group by its key. This sealed
 * class serves as a base class for more specific exceptions that define particular failure
 * scenarios related to group deletion processes.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class DeleteGroupByKeyException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a specific exception that occurs when attempting to delete a group by its key,
     * but no group with the specified key exists. This exception extends [DeleteGroupByKeyException]
     * to provide additional context to the error.
     *
     * @param msg A detailed message describing the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : DeleteGroupByKeyException(
        msg,
        "GROUP_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "No group with specified key found.",
        cause
    )

    /**
     * Exception used to indicate a failure when attempting to retrieve a group from the database
     * during the process of deleting a group by its key.
     *
     * This exception extends the [DeleteGroupByKeyException] to provide specific details about
     * database-related issues encountered.
     *
     * @param msg A descriptive message providing context about the database failure.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_DB_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : DeleteGroupByKeyException(
        msg,
        "GROUP_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve group from database.",
        cause
    )

    /**
     * Exception used to indicate a failure when attempting to update a member after the deletion
     * of a group. This specific exception extends `DeleteGroupByKeyException` to provide detailed
     * information about the failure encountered during the member update process.
     *
     * @param msg A descriptive message explaining the context of the failure.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `MEMBER_UPDATE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class MemberUpdate(msg: String, cause: Throwable? = null) : DeleteGroupByKeyException(
        msg,
        "MEMBER_UPDATE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to update member after group deletion.",
        cause
    )
}
