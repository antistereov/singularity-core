package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class TotpException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class GenerateSecret(msg: String, cause: Throwable? = null) : TotpException(
        msg,
        "GENERATE_TOTP_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to generate TOTP secret.",
        cause
    )

    class Validation(msg: String, cause: Throwable? = null) : TotpException(
        msg,
        "TOTP_CODE_VALIDATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to validate TOTP code.",
        cause
    )

    class GeneratePassword(msg: String, cause: Throwable? = null) : TotpException(
        msg,
        "GENERATE_TOTP_PASSWORD_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to get password.",
        cause
    )
}
