package io.stereov.web.user.model

import io.stereov.web.user.exception.InvalidRoleException

enum class Role(private val value: String) {
    USER("USER"),
    ADMIN("ADMIN"),
    GUEST("GUEST");

    override fun toString(): String {
        return this.value
    }

    companion object {
        fun fromString(role: String): Role {
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
