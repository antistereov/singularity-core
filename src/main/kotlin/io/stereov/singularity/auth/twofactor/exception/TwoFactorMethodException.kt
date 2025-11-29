package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class TwoFactorMethodException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Invalid(msg: String, cause: Throwable? = null) : TwoFactorMethodException(
        msg,
        "INVALID_TWO_FACTOR_METHOD",
        HttpStatus.BAD_REQUEST,
        "Invalid two factor method.",
        cause
    )
}