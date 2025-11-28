package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ChangePreferredTwoFactorMethodException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {


    class Database(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class TwoFactorDisabled(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        "TWO_FACTOR_DISABLED",
        HttpStatus.BAD_REQUEST,
        "Two factor authentication is disabled for this user.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "An error occurred during after the operation was successfully committed.",
        cause
    )
}
