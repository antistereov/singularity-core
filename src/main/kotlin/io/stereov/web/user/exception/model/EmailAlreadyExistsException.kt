package io.stereov.web.user.exception.model

import io.stereov.web.user.exception.UserException

/**
 * # Exception thrown when a user tries to register with an email that already exists.
 *
 * This exception is thrown when a user attempts to register with an email address that is already associated
 * with an existing account.
 *
 * @param info Additional information about the exception.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class EmailAlreadyExistsException(info: String) : UserException(
    message = "$info: Email already exists"
)
