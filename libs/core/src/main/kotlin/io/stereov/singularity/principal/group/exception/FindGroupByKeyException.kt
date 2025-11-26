package io.stereov.singularity.principal.group.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the process of finding a group by its key.
 * This sealed class serves as a base for more specific exceptions that handle particular
 * failure scenarios related to this operation.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class FindGroupByKeyException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception used to indicate that a group with the specified key was not found.
     *
     * This exception extends [FindGroupByKeyException] and is specifically intended to represent
     * scenarios where the requested group cannot be located in the system.
     *
     * @param msg A descriptive message providing context for the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : FindGroupByKeyException(
        msg,
        "GROUP_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "No group with specified key found.",
        cause
    )

    /**
     * Represents a database-related exception occurring when attempting to retrieve a group.
     *
     * This exception is a specific type of [FindGroupByKeyException] intended to indicate failures
     * that are directly related to interactions with the database, such as connectivity issues or
     * data retrieval failures.
     *
     * @param msg A descriptive message providing additional context about the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GROUP_DB_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : FindGroupByKeyException(
        msg,
        "GROUP_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve group from database.",
        cause
    )
}
