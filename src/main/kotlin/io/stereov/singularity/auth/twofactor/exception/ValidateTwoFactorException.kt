package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ValidateTwoFactorException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class UserNotFound(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "USER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "User not found.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class TwoFactorDisabled(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "TWO_FACTOR_DISABLED",
        HttpStatus.BAD_REQUEST,
        "Two factor authentication is disabled for this user.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class InvalidRequest(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "INVALID_REQUEST",
        HttpStatus.BAD_REQUEST,
        "Invalid request: at least one of email or totp must be provided.",
        cause
    )

    class Expired(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "TWO_FACTOR_CODE_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Two factor code has expired.",
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "WRONG_TWO_FACTOR_CODE",
        HttpStatus.UNAUTHORIZED,
        "Wrong two factor code.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "An error occurred during after the operation was successfully committed.",
        cause
    )

    class Totp(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "TOTP_CODE_VALIDATION_FAILURE",
        HttpStatus.UNAUTHORIZED,
        "Failed to validate TOTP code.",
        cause
    )

    companion object {

        fun fromValidateEmailTwoFactorCode(ex: ValidateEmailTwoFactorCodeException): ValidateTwoFactorException {
            return when (ex) {
                is ValidateEmailTwoFactorCodeException.WrongCode -> WrongCode(ex.message, ex.cause)
                is ValidateEmailTwoFactorCodeException.Expired -> Expired(ex.message, ex.cause)
            }
        }
    }
}
