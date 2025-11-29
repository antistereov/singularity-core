package io.stereov.singularity.content.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DeleteContentInvitationException(
    msg: String, 
    code: String, 
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class ContentNotFound(msg: String, cause: Throwable? = null): DeleteContentInvitationException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : DeleteContentInvitationException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )


    companion object {

        fun from(ex: FindDocumentByKeyException): DeleteContentInvitationException {
            return when (ex) {
                is FindDocumentByKeyException.Database -> Database(ex.message, ex.cause)
                is FindDocumentByKeyException.NotFound -> ContentNotFound(ex.message, ex.cause)
            }
        }
    }
}
