package io.stereov.singularity.user.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class FindPrincipalByIdException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    class NotFound(msg: String, cause: Throwable? = null) : FindPrincipalByIdException(
        msg,
        "PRINCIPAL_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "No principal with specified email found.",
        cause
    )

    class HashFailure(msg: String, cause: Throwable? = null) : FindPrincipalByIdException(
        msg,
        "PRINCIPAL_HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to generate or verify hash.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : FindPrincipalByIdException(
        msg,
        "PRINCIPAL_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve principal from database.",
        cause
    )

    class Encryption(msg: String, cause: Throwable? = null) : FindPrincipalByIdException(
        msg,
        "PRINCIPAL_ENCRYPTION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to encrypt or decrypt principal data.",
        cause
    )
}
