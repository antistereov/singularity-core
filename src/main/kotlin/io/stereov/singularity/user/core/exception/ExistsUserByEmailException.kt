package io.stereov.singularity.user.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ExistsUserByEmailException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    class HashFailure(msg: String, cause: Throwable? = null) : ExistsUserByEmailException(
        msg,
        "USER_HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to generate or verify hash.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : ExistsUserByEmailException(
        msg,
        "USER_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve user from database.",
        cause
    )
}
