package io.stereov.singularity.content.tag.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class CreateTagException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : CreateTagException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class KeyExists(msg: String, cause: Throwable? = null) : CreateTagException(
        msg,
        "TAG_EXISTS",
        HttpStatus.CONFLICT,
        "A tag with the specified key already exists.",
        cause
    )
}