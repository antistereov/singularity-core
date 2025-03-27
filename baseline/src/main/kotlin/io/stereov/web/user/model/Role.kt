package io.stereov.web.user.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.user.exception.InvalidRoleException

enum class Role(private val value: String) {
    USER("USER"),
    ADMIN("ADMIN"),
    GUEST("GUEST");

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override fun toString(): String {
        logger.debug { "Returning role as string" }

        return this.value
    }

    companion object {
        private val logger: KLogger
            get() = KotlinLogging.logger {}

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
