package io.stereov.singularity.core.user.exception.model

import io.stereov.singularity.core.user.exception.UserException

/**
 * # Invalid role exception.
 *
 * This exception is thrown when a user tries to access a resource with an invalid role.
 *
 * @param role The invalid role that was attempted to be used.
 * @param cause The cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class InvalidRoleException(role: String, cause: Throwable? = null) : UserException("Role $role does not exist or cannot be identified correctly", cause)
