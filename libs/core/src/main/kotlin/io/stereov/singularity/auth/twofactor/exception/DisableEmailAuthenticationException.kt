package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DisableEmailAuthenticationException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyDisabled(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        "TWO_FACTOR_ALREADY_DISABLED",
        HttpStatus.NOT_MODIFIED,
        "Two factor authentication is already disabled for this user.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class Expired(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        "TWO_FACTOR_CODE_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Two factor code has expired.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "An error occurred during after the operation was successfully committed.",
        cause
    )

    class CannotDisableOnlyTwoFactorMethod(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        "CANNOT_DISABLE_ONLY_TWO_FACTOR_METHOD",
        HttpStatus.BAD_REQUEST,
        "Cannot disable the configured only two factor method.",
        cause
    )

}
