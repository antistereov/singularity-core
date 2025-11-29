package io.stereov.singularity.content.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.InvalidDocumentFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class GenerateExtendedContentAccessDetailsException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : GenerateExtendedContentAccessDetailsException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NotAuthorized(msg: String, cause: Throwable? = null) : GenerateExtendedContentAccessDetailsException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class ContentNotFound(msg: String, cause: Throwable? = null): GenerateExtendedContentAccessDetailsException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null): GenerateExtendedContentAccessDetailsException(
        msg,
        InvalidDocumentFailure.CODE,
        InvalidDocumentFailure.STATUS,
        InvalidDocumentFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: FindContentAuthorizedException): GenerateExtendedContentAccessDetailsException {
            return when (ex) {
                is FindContentAuthorizedException.Database -> Database(ex.message, ex.cause)
                is FindContentAuthorizedException.NotFound -> ContentNotFound(ex.message, ex.cause)
                is FindContentAuthorizedException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
            }
        }
    }
}