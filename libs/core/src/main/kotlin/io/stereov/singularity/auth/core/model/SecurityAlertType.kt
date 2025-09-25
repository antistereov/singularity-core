package io.stereov.singularity.auth.core.model

enum class SecurityAlertType(val value: String) {
    EMAIL_CHANGED("email_changed"),
    PASSWORD_CHANGED("password_changed"),
    TWO_FACTOR_ADDED("2fa_added"),
    TWO_FACTOR_REMOVED("2fa_removed"),
    OAUTH_CONNECTED("oauth_connected"),
    OAUTH_DISCONNECTED("oauth_disconnected");

    override fun toString(): String {
        return value
    }
}