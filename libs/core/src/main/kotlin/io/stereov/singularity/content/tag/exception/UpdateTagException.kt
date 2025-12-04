package io.stereov.singularity.content.tag.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class UpdateTagException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : UpdateTagException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NotFound(msg: String, cause: Throwable? = null) : UpdateTagException(
        msg,
        "TAG_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "The tag does not exist.",
        cause
    )

    class InvalidRequest(msg: String, cause: Throwable? = null) : UpdateTagException(
        msg,
        "INVALID_TAG_REQUEST",
        HttpStatus.BAD_REQUEST,
        "The request is invalid.",
        cause
    )
}