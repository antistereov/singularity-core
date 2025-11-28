package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ValidateEmailTwoFactorCodeException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Expired(msg: String, cause: Throwable? = null) : ValidateEmailTwoFactorCodeException(
        msg,
        "TWO_FACTOR_CODE_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Two factor code has expired.",
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : ValidateEmailTwoFactorCodeException(
        msg,
        "WRONG_TWO_FACTOR_CODE",
        HttpStatus.UNAUTHORIZED,
        "Wrong two factor code.",
        cause
    )
}
