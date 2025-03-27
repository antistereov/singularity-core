package io.stereov.web.user.exception

class InvalidRoleException(role: String, cause: Throwable? = null) : UserException("Role $role does not exist or cannot be identified correctly", cause)
