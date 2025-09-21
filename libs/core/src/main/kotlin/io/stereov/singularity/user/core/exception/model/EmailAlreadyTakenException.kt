package io.stereov.singularity.user.core.exception.model

import io.stereov.singularity.user.core.exception.UserException

class EmailAlreadyTakenException(info: String) : UserException(
    message = "$info: Email already exists"
)
