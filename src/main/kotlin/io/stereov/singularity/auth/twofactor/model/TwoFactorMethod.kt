package io.stereov.singularity.auth.twofactor.model

enum class TwoFactorMethod(val value: String) {

    TOTP("totp"),
    EMAIL("mail");

    override fun toString(): String {
        return value
    }
}