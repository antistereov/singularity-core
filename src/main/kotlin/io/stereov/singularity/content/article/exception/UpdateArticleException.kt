package io.stereov.singularity.content.article.exception

import io.stereov.singularity.content.core.exception.ContentNotFoundFailure
import io.stereov.singularity.content.core.exception.FindContentAuthorizedException
import io.stereov.singularity.content.core.exception.NotAuthorizedFailure
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.InvalidDocumentFailure
import io.stereov.singularity.global.exception.InvalidRequestFailure
import io.stereov.singularity.global.exception.ResponseMappingFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.translate.exception.TranslateException
import org.springframework.http.HttpStatus

sealed class UpdateArticleException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : UpdateArticleException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NotAuthorized(msg: String, cause: Throwable? = null) : UpdateArticleException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class ContentNotFound(msg: String, cause: Throwable? = null): UpdateArticleException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null): UpdateArticleException(
        msg,
        InvalidDocumentFailure.CODE,
        InvalidDocumentFailure.STATUS,
        InvalidDocumentFailure.DESCRIPTION,
        cause
    )

    class InvalidRequest(msg: String, cause: Throwable? = null) : UpdateArticleException(
        msg,
        InvalidRequestFailure.CODE,
        InvalidRequestFailure.STATUS,
        InvalidRequestFailure.DESCRIPTION,
        cause
    )

    class NoTranslations(msg: String, cause: Throwable? = null) : UpdateArticleException(
        msg,
        TranslateException.NoTranslations.CODE,
        TranslateException.NoTranslations.STATUS,
        TranslateException.NoTranslations.DESCRIPTION,
        cause
    )

    class ResponseMapping(msg: String, cause: Throwable? = null) : UpdateArticleException(
        msg,
        ResponseMappingFailure.CODE,
        ResponseMappingFailure.STATUS,
        ResponseMappingFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: FindContentAuthorizedException): UpdateArticleException {
            return when (ex) {
                is FindContentAuthorizedException.Database -> Database(ex.message, ex.cause)
                is FindContentAuthorizedException.NotFound -> ContentNotFound(ex.message, ex.cause)
                is FindContentAuthorizedException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
            }
        }

        fun from(ex: GetUniqueArticleKeyException): UpdateArticleException {
            return when (ex) {
                is GetUniqueArticleKeyException.Database -> Database(ex.message, ex.cause)
                is GetUniqueArticleKeyException.InvalidDocument -> InvalidDocument(ex.message, ex.cause)
            }
        }
    }
}