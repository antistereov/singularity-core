package io.stereov.singularity.user.core.dto.response

import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.user.core.model.Role
import org.bson.types.ObjectId

/**
 * # User Data Transfer Object (DTO).
 *
 * This data class represents a user in the system.
 * It contains various properties such as user ID, name, email,
 * roles, email verification status, session information,
 * last active time, two-factor authentication status,
 * and application information.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user (nullable).
 * @property email The email address of the user.
 * @property roles The list of roles assigned to the user (default is a list containing the USER role).
 * @property emailVerified Indicates whether the user's email is verified (default is false).
 * @property lastActive The last active time of the user.
 * @property twoFactorAuthEnabled Indicates whether two-factor authentication is enabled for the user.
 * @property avatar The URL for the user's avatar. Is null if the user did not set a custom avatar.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class UserResponse(
    val id: ObjectId,
    val name: String,
    val email: String,
    val roles: Set<Role> = setOf(Role.USER),
    val emailVerified: Boolean = false,
    val lastActive: String,
    val twoFactorAuthEnabled: Boolean,
    val avatar: FileMetadataResponse?,
    val created: String,
    val groups: Set<String>,
)
