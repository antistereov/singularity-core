package io.stereov.singularity.auth.twofactor.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.stereov.singularity.auth.twofactor.exception.TwoFactorMethodException

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
        @Suppress("UNUSED")
        fun ofString(input: String): Result<TwoFactorMethod, TwoFactorMethodException> {
            return when (input.lowercase()) {
                "totp" -> Ok(TOTP)
                "email" -> Ok(EMAIL)
                else -> Err(TwoFactorMethodException.Invalid("No two-factor authentication method \"$input\" found"))
            }
        }
    }
}