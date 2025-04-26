package io.stereov.singularity.core.user.exception.model

import io.stereov.singularity.core.user.exception.UserException

/**
 * # Exception thrown when a user does not exist.
 *
 * This exception is thrown when a user account is not found in the system.
 *
 * @param message The message to be displayed when the exception is thrown.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class UserDoesNotExistException(
    message: String
) : UserException(
    message = message
)
