package io.stereov.singularity.user.core.exception.model

import io.stereov.singularity.user.core.exception.UserException

class EmailAlreadyExistsException(info: String) : UserException(
    message = "$info: Email already exists"
)
