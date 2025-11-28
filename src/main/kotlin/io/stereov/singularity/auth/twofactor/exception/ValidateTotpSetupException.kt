package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ValidateTotpSetupException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyEnabled(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "TWO_FACTOR_ALREADY_ENABLED",
        HttpStatus.NOT_MODIFIED,
        "Two factor authentication is already enabled for this user.",
        cause
    )

    class Totp(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "TOTP_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with TOTP fails.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )


    class Database(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with database fails.",
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "WRONG_CODE",
        HttpStatus.UNAUTHORIZED,
        "Wrong TOTP code.",
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with hash fails.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a side effect fails after the operation was successfully committed.",
        cause
    )
}
