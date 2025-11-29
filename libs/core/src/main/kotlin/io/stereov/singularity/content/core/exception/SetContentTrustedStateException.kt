package io.stereov.singularity.content.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class SetContentTrustedStateException(
    msg: String, 
    code: String, 
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class ContentNotFound(msg: String, cause: Throwable? = null): SetContentTrustedStateException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : SetContentTrustedStateException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class ResponseMapping(msg: String, cause: Throwable? = null) : SetContentTrustedStateException(
        msg,
        "RESPONSE_MAPPING_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to map response.",
        cause
    )


    companion object {

        fun from(ex: FindDocumentByKeyException): SetContentTrustedStateException {
            return when (ex) {
                is FindDocumentByKeyException.Database -> Database(ex.message, ex.cause)
                is FindDocumentByKeyException.NotFound -> ContentNotFound(ex.message, ex.cause)
            }
        }
    }
}
