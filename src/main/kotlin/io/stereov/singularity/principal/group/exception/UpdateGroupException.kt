package io.stereov.singularity.principal.group.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the update of a group. This sealed class serves as
 * a base class for more specific exceptions that define particular failure scenarios related to
 * group update processes.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class UpdateGroupException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception used to indicate that a group with the specified key could not be found.
     *
     * This exception extends the [UpdateGroupException] to signal failures related to the
     * absence of a group matching the provided key.
     *
     * @param msg A descriptive message explaining the issue encountered.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : UpdateGroupException(
        msg,
        "GROUP_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "No group with specified key found.",
        cause
    )

    /**
     * Exception used to indicate that an invalid group translation was encountered during
     * the process of updating a group.
     *
     * This exception extends the [UpdateGroupException] to provide a specific context for
     * failures related to group translation. It signifies that the given group translation
     * does not conform to expected standards or requirements.
     *
     * @param msg A descriptive message explaining the nature of the invalid group translation.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `INVALID_GROUP_TRANSLATION`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class InvalidGroupTranslation(msg: String, cause: Throwable? = null) : UpdateGroupException(
        msg,
        "INVALID_GROUP_TRANSLATION",
        HttpStatus.BAD_REQUEST,
        "Invalid group translation.",
        cause
    )

    /**
     * Exception used to indicate a failure when attempting to retrieve a group from the database.
     *
     * This exception extends the [UpdateGroupException] to provide specific details about
     * database-related issues encountered during the group update process.
     *
     * @param msg A descriptive message providing context about the database failure.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_DB_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : UpdateGroupException(
        msg,
        "GROUP_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve group from database.",
        cause
    )
}
