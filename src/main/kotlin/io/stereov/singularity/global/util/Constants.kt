package io.stereov.singularity.global.util

object Constants {
    const val JWT_ROLES_CLAIM = "roles"
    const val JWT_GROUPS_CLAIM = "groups"
    const val JWT_SESSION_CLAIM = "session"
    const val JWT_BROWSER_CLAIM = "browser"
    const val JWT_OS_CLAIM = "os"
    const val JWT_OAUTH2_PROVIDER_CLAIM = "oauth2_provider"
    const val TWO_FACTOR_SECRET_CLAIM = "2fa_secret"
    const val TWO_FACTOR_RECOVERY_CLAIM = "2fa_recovery"

    const val SESSION_TOKEN_PARAMETER = "session_token"
    const val REDIRECT_URI_PARAMETER = "redirect_uri"
    const val STEP_UP_TOKEN_PARAMETER = "step_up_token"
    const val OAUTH2_PROVIDER_CONNECTION_TOKEN_PARAMETER = "oauth2_provider_connection_token"
    const val STEP_UP_PARAMETER = "step_up"

    const val ENCRYPTION_SECRET = "encryption-secret"
    const val JWT_SECRET = "jwt-secret"
    const val HASH_SECRET = "hash-secret"
}
