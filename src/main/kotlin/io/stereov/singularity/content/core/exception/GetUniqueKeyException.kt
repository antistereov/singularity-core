package io.stereov.singularity.content.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class GetUniqueKeyException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : GetUniqueKeyException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )
}