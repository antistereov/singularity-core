package io.stereov.web.user.exception

class EmailAlreadyExistsException(info: String) : UserException(
    message = "$info: Email already exists"
)
