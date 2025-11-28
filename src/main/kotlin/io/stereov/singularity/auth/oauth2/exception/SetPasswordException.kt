package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class SetPasswordException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class PasswordAlreadySet(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        "PASSWORD_ALREADY_SET",
        HttpStatus.NOT_MODIFIED,
        "Password is already set.",
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        "HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to hash password.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to save password hash to database.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to execute post commit side effect.",
        cause
    )
}