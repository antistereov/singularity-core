package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class TotpUserRecoveryException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class UserNotFound(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "USER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "User not found.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with database fails.",
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "WRONG_CODE",
        HttpStatus.UNAUTHORIZED,
        "Wrong TOTP recovery code.",
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an operation with hash fails.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a side effect fails after the operation was successfully committed.",
        cause
    )

    class MethodDisabled(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        "TWO_FACTOR_METHOD_DISABLED",
        HttpStatus.BAD_REQUEST,
        "Two factor authentication method is disabled.",
        cause
    )
}
