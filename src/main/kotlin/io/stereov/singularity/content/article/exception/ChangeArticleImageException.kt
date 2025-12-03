package io.stereov.singularity.content.article.exception

import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.content.core.exception.ContentNotFoundFailure
import io.stereov.singularity.content.core.exception.FindContentAuthorizedException
import io.stereov.singularity.content.core.exception.NotAuthorizedFailure
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.InvalidDocumentFailure
import io.stereov.singularity.file.core.exception.FileFailure
import io.stereov.singularity.file.core.exception.UnsupportedMediaTypeFailure
import io.stereov.singularity.global.exception.ResponseMappingFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.translate.exception.TranslateException
import org.springframework.http.HttpStatus

sealed class ChangeArticleImageException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : ChangeArticleImageException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NotAuthenticated(msg: String, cause: Throwable? = null) : ChangeArticleImageException(
        msg,
        AuthenticationException.AuthenticationRequired.CODE,
        AuthenticationException.AuthenticationRequired.STATUS,
        AuthenticationException.AuthenticationRequired.DESCRIPTION,
        cause
    )

    class NotAuthorized(msg: String, cause: Throwable? = null) : ChangeArticleImageException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class ContentNotFound(msg: String, cause: Throwable? = null): ChangeArticleImageException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null): ChangeArticleImageException(
        msg,
        InvalidDocumentFailure.CODE,
        InvalidDocumentFailure.STATUS,
        InvalidDocumentFailure.DESCRIPTION,
        cause
    )

    class NoTranslations(msg: String, cause: Throwable? = null) : ChangeArticleImageException(
        msg,
        TranslateException.NoTranslations.CODE,
        TranslateException.NoTranslations.STATUS,
        TranslateException.NoTranslations.DESCRIPTION,
        cause
    )

    class File(msg: String, cause: Throwable? = null) : ChangeArticleImageException(
        msg,
        FileFailure.CODE,
        FileFailure.STATUS,
        FileFailure.DESCRIPTION,
        cause
    )

    class ResponseMapping(msg: String, cause: Throwable? = null) : ChangeArticleImageException(
        msg,
        ResponseMappingFailure.CODE,
        ResponseMappingFailure.STATUS,
        ResponseMappingFailure.DESCRIPTION,
        cause
    )

    class UnsupportedMediaType(msg: String, cause: Throwable? = null) : ChangeArticleImageException(
        msg,
        UnsupportedMediaTypeFailure.CODE,
        UnsupportedMediaTypeFailure.STATUS,
        UnsupportedMediaTypeFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: FindContentAuthorizedException): ChangeArticleImageException {
            return when (ex) {
                is FindContentAuthorizedException.Database -> Database(ex.message, ex.cause)
                is FindContentAuthorizedException.NotFound -> ContentNotFound(ex.message, ex.cause)
                is FindContentAuthorizedException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
                is FindContentAuthorizedException.NotAuthenticated -> NotAuthenticated(ex.message, ex.cause)
            }
        }

        fun from(ex: GetUniqueArticleKeyException): ChangeArticleImageException {
            return when (ex) {
                is GetUniqueArticleKeyException.Database -> Database(ex.message, ex.cause)
                is GetUniqueArticleKeyException.InvalidDocument -> InvalidDocument(ex.message, ex.cause)
            }
        }
    }
}