package io.stereov.singularity.auth.twofactor.model

import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorMethodException

enum class TwoFactorMethod(val value: String) {

    TOTP("totp"),
    EMAIL("email");

    override fun toString(): String {
        return value
    }

    companion object {
        fun ofString(input: String): TwoFactorMethod {
            return when (input.lowercase()) {
                "totp" -> TOTP
                "email" -> EMAIL
                else -> throw InvalidTwoFactorMethodException(input)
            }
        }
    }
}