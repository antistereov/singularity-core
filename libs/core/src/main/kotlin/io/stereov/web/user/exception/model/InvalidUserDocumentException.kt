package io.stereov.web.user.exception.model

import io.stereov.web.user.exception.UserException

/**
 * # Invalid user document exception.
 *
 * This exception is thrown when a user document is invalid.
 *
 * @param message The error message.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class InvalidUserDocumentException(
    message: String
) : UserException(
    message = message
)
