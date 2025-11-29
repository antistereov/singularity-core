package io.stereov.singularity.principal.group.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the creation of a group. This sealed class serves as
 * a base class for more specific exceptions that define particular failure scenarios related to
 * group creation processes.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class CreateGroupException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * A specific exception that represents an error caused by invalid group translation during
     * the creation of a group. It extends the [CreateGroupException] to provide more context
     * about the nature of the failure.
     *
     * @param msg A detailed message explaining the invalid group translation error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `INVALID_GROUP_TRANSLATION`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class InvalidGroupTranslation(msg: String, cause: Throwable? = null) : CreateGroupException(
        msg,
        "INVALID_GROUP_TRANSLATION",
        HttpStatus.BAD_REQUEST,
        "Invalid group translation.",
        cause
    )

    /**
     * Exception used to indicate that a group key already exists during the creation of a group.
     *
     * This exception extends the [CreateGroupException] to provide detailed information
     * about the conflict caused by duplicate group keys.
     *
     * @param msg A descriptive message explaining the conflict encountered.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_KEY_EXISTS`
     * @property status [HttpStatus.CONFLICT]
     */
    class KeyExists(msg: String, cause: Throwable? = null) : CreateGroupException(
        msg,
        "GROUP_KEY_EXISTS",
        HttpStatus.CONFLICT,
        "Group key already exists.",
        cause
    )

    /**
     * Exception used to indicate a failure when attempting to retrieve a group from the database.
     *
     * This exception extends the [CreateGroupException] to provide specific details about
     * database-related issues encountered during the group creation process.
     *
     * @param msg A descriptive message providing context about the database failure.
     * @param cause The optional underlying cause of the exception.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : CreateGroupException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )
}
