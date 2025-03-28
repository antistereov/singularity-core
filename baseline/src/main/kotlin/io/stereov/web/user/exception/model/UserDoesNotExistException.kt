package io.stereov.web.user.exception.model

import io.stereov.web.user.exception.UserException

class UserDoesNotExistException(
    message: String
) : UserException(
    message = message
)
