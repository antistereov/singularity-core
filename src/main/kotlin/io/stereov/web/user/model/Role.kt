package io.stereov.web.user.model

enum class Role(private val value: String) {
    USER("USER"),
    ADMIN("ADMIN");

    override fun toString(): String {
        return this.value
    }
}
