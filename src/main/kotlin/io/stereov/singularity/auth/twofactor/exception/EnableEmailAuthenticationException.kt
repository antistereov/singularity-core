package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class EnableEmailAuthenticationException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyEnabled(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        "TWO_FACTOR_ALREADY_ENABLED",
        HttpStatus.NOT_MODIFIED,
        "Two factor authentication is already enabled for this user.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class Expired(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        "TWO_FACTOR_CODE_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Two factor code has expired.",
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        "WRONG_TWO_FACTOR_CODE",
        HttpStatus.UNAUTHORIZED,
        "Wrong two factor code.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "An error occurred during after the operation was successfully committed.",
        cause
    )

    companion object {

        fun fromValidateEmailTwoFactorCode(ex: ValidateEmailTwoFactorCodeException): EnableEmailAuthenticationException {
            return when (ex) {
                is ValidateEmailTwoFactorCodeException.WrongCode -> WrongCode(ex.message, ex.cause)
                is ValidateEmailTwoFactorCodeException.Expired -> Expired(ex.message, ex.cause)
            }
        }
    }

}
