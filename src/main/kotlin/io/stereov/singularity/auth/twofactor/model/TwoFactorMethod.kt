package io.stereov.singularity.auth.twofactor.model

enum class TwoFactorMethod(val value: String) {

    TOTP("totp"),
    MAIL("mail");

    override fun toString(): String {
        return value
    }
}