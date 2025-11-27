package io.stereov.singularity.auth.core.model

/**
 * Enum class that represents different types of security alerts.
 *
 * Security alerts provide notifications for specific security-related events
 * that occur within the system. These events include actions such as changes
 * to user email or password, modifications to two-factor authentication setup,
 * or changes to connected third-party OAuth services. Each type of alert
 * is associated with a unique string value.
 *
 * @property value The string representation of the security alert type.
 */
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
