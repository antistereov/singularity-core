package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class GenerateTotpDetailsException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyEnabled(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "TWO_FACTOR_ALREADY_ENABLED",
        HttpStatus.NOT_MODIFIED,
        "Two factor authentication is already enabled for this user.",
        cause
    )

    class Totp(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "TOTP_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with TOTP fails.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class TokenCreation(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "TOTP_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to create TOTP token.",
        cause
    )

    class InvalidConfiguration(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "INVALID_CONFIGURATION",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid configuration.",
        cause
    )
}
