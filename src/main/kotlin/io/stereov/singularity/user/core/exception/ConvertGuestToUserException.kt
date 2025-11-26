package io.stereov.singularity.user.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ConvertGuestToUserException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    class IsAlreadyUser(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "GUEST_IS_ALREADY_USER",
        HttpStatus.BAD_REQUEST,
        "Cannot convert guest to user because it is already a user.",
        cause
    )

    class EmailTaken(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "GUEST_EMAIL_TAKEN",
        HttpStatus.CONFLICT,
        "Cannot convert guest to user because the email address is already taken.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "USER_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve guest from database.",
        cause
    )

    class NotFound(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "GUEST_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Cannot convert guest to user because it does not exist.",
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "GUEST_HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to generate or verify hash.",
        cause
    )
}
