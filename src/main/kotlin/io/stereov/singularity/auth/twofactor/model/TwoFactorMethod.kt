package io.stereov.singularity.auth.twofactor.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorMethodException

enum class TwoFactorMethod(val value: String) {

    TOTP("totp"),
    EMAIL("email");

    @JsonValue
    override fun toString(): String {
        return value
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun ofString(input: String): TwoFactorMethod {
            return when (input.lowercase()) {
                "totp" -> TOTP
                "email" -> EMAIL
                else -> throw InvalidTwoFactorMethodException(input)
            }
        }
    }
}