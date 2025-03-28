package io.stereov.web.user.exception.model

import io.stereov.web.user.exception.UserException

class InvalidRoleException(role: String, cause: Throwable? = null) : UserException("Role $role does not exist or cannot be identified correctly", cause)
