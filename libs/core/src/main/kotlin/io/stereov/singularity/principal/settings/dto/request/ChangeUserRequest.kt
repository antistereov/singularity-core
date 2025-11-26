package io.stereov.singularity.principal.settings.dto.request

/**
 * # Change user request.
 *
 * This data class represents a request to change user information.
 * It contains a single property, `name`, which is a string representing the new name of the user.
 *
 * @property name The new name of the user.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class ChangeUserRequest(
    val name: String?,
)
