package io.stereov.singularity.global.util

object Constants {
    const val ACCESS_TOKEN_COOKIE = "access_token"
    const val REFRESH_TOKEN_COOKIE = "refresh_token"
    const val LOGIN_VERIFICATION_TOKEN_COOKIE = "login_verification_token"
    const val TWO_FACTOR_SETUP_TOKEN_COOKIE = "2fa_setup_token"
    const val STEP_UP_TOKEN_COOKIE = "step_up_token"

    const val JWT_DEVICE_CLAIM = "device"
    const val TWO_FACTOR_SECRET_CLAIM = "2fa_secret"
    const val TWO_FACTOR_RECOVERY_CLAIM = "2fa_recovery"

    const val ENCRYPTION_SECRET = "encryption-secret"
    const val JWT_SECRET = "jwt-secret"
    const val HASH_SECRET = "hash-secret"
}
