package io.stereov.web.user.exception.model

import io.stereov.web.user.exception.UserException

class EmailAlreadyExistsException(info: String) : UserException(
    message = "$info: Email already exists"
)
