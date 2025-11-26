package io.stereov.singularity.user.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to role operations.
 *
 * This sealed class serves as a base for specific exceptions that may occur
 * when handling role-related operations. It extends [SingularityException]
 * to provide a consistent structure for error details, including a message,
 * error code, HTTP status, detailed description, and optionally a root cause.
 *
 * @param msg The error message providing context for the exception.
 * @param code A unique code identifying the specific type of role-related error.
 * @param status The corresponding HTTP status that represents this error.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class RoleException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing an invalid role creation attempt.
     *
     * This exception is thrown when attempting to create a `Role` from a string
     * that does not match any predefined or existing roles. It extends the
     * `RoleException` to provide a consistent structure for role-related errors.
     *
     * @param role The input string representing the role that could not be mapped.
     *
     * @property code `INVALID_ROLE`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class Invalid(role: String) : RoleException(
        "Failed to create role based on input $role",
        "INVALID_ROLE",
        HttpStatus.BAD_REQUEST,
        "Thrown when trying to create a Role based on a string that does not match any existing roles.",
        null
    )

}
