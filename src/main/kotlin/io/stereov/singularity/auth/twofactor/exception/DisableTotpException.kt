package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DisableTotpException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyDisabled(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "TWO_FACTOR_ALREADY_DISABLED",
        HttpStatus.NOT_MODIFIED,
        "Two factor authentication is already disabled for this user.",
        cause
    )

    class CannotDisableOnlyTwoFactorMethod(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "CANNOT_DISABLE_ONLY_TWO_FACTOR_METHOD",
        HttpStatus.BAD_REQUEST,
        "Cannot disable the configured only two factor method.",
        cause
    )

    class UserNotFound(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "USER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "User not found.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with database fails.",
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with hash fails.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a side effect fails after the operation was successfully committed.",
        cause
    )
}
