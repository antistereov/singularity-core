package io.stereov.singularity.user.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.user.exception.model.InvalidRoleException
import io.stereov.singularity.user.model.Role.*

/**
 * # Enum class representing the roles of a user in the system.
 *
 * This class is used to define the different roles that a user can have in the system.
 *
 * ## Roles:
 * - [USER]: Represents a regular user.
 * - [ADMIN]: Represents an administrator user.
 * - [GUEST]: Represents a guest user.
 *
 * @property value The string representation of the role.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
enum class Role(private val value: String) {
    /**
     * Represents a regular user.
     */
    USER("USER"),
    /**
     * Represents an administrator user.
     */
    ADMIN("ADMIN"),
    /**
     * Represents a guest user.
     */
    GUEST("GUEST");

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Returns the string representation of the role.
     *
     * @return The string representation of the role.
     */
    override fun toString(): String {
        logger.debug { "Returning role as string" }

        return this.value
    }

    companion object {
        private val logger: KLogger
            get() = KotlinLogging.logger {}

        /**
         * Creates a [Role] from a string representation.
         *
         * This method converts the string to lowercase and matches it with the enum values.
         *
         * @param role The string representation of the role.
         *
         * @return The corresponding [Role] enum value.
         *
         * @throws InvalidRoleException If the string does not match any role.
         */
        fun fromString(role: String): Role {
            logger.debug { "Creating role from string: $role" }

            val roleLowerCase = role.lowercase()

            return when(roleLowerCase) {
                "user" -> USER
                "admin" -> ADMIN
                "guest" -> GUEST
                else -> throw InvalidRoleException(role)
            }
        }
    }
}
