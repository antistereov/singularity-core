package io.stereov.singularity.principal.core.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.config.JsonConfiguration
import io.stereov.singularity.principal.core.exception.RoleException

/**
 * Represents a role within the system.
 *
 * The sealed [Role] interface contains two enum classes that represent all available roles:
 * * [User]-related roles: [User.ADMIN] and [User.USER]
 * * [Guest]-related roles: [Guest.GUEST]
 *
 * Roles define the level of access and permissions
 * a user or entity has in the system.
 */
@JsonDeserialize(using = JsonConfiguration.RoleDeserializer::class)
sealed interface Role {

    val logger: KLogger
    @get:JsonValue
    val value: String

    /**
     * Enum class representing different user roles in the system.
     *
     * Implements the Role interface to provide role-specific behavior and ensures logging functionality.
     */
    enum class User(override val value: String) : Role {
        /**
         * Represents a regular user.
         */
        USER("USER"),
        /**
         * Represents an administrator user.
         */
        ADMIN("ADMIN");

        override val logger = KotlinLogging.logger {}
        override fun toString() = value
    }

    /**
     * Defines a specific implementation of the Role interface for a Guest user.
     *
     * This enum provides the designation for a user with guest privileges and functionality,
     * and implements behavior defined by the Role interface.
     *
     * @property value The string representation of the role, which is "GUEST" for this type.
     */
    enum class Guest(override val value: String) : Role {
        /**
         * Represents a guest user.
         */
        GUEST("GUEST");

        override val logger = KotlinLogging.logger {}
        override fun toString() = value
    }

    companion object {
        private val logger: KLogger
            get() = KotlinLogging.logger {}

        /**
         * Converts an input string into a corresponding [Role] instance.
         * If the input does not match any existing role, an [RoleException.Invalid] is returned.
         *
         * @param input The string representation of the role to be converted.
         *   Accepted values are case-insensitive and include "user", "admin", and "guest".
         * @return A [Result] containing the corresponding [Role] if the input is valid,
         *  or an [RoleException.Invalid] if the input does not match any existing roles.
         */
        fun fromString(input: String): Result<Role, RoleException.Invalid> {
            logger.debug { "Creating role from string: $input" }

            val roleLowerCase = input.lowercase()

            return when(roleLowerCase) {
                "user" -> Ok(User.USER)
                "admin" -> Ok(User.ADMIN)
                "guest" -> Ok(Guest.GUEST)
                else -> Err(RoleException.Invalid(input))
            }
        }
    }
}
