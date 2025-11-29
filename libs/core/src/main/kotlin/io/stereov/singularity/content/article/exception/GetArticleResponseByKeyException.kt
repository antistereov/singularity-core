package io.stereov.singularity.content.article.exception

import io.stereov.singularity.content.core.exception.ContentNotFoundFailure
import io.stereov.singularity.content.core.exception.FindContentAuthorizedException
import io.stereov.singularity.content.core.exception.NotAuthorizedFailure
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.file.core.exception.FileFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.translate.exception.TranslateException
import org.springframework.http.HttpStatus

sealed class GetArticleResponseByKeyException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class NoTranslations(msg: String, cause: Throwable? = null) : GetArticleResponseByKeyException(
        msg,
        TranslateException.NoTranslations.CODE,
        TranslateException.NoTranslations.STATUS,
        TranslateException.NoTranslations.DESCRIPTION,
        cause
    )

    class File(msg: String, cause: Throwable? = null) : GetArticleResponseByKeyException(
        msg,
        FileFailure.CODE,
        FileFailure.STATUS,
        FileFailure.DESCRIPTION,
        cause
    )

    class OwnerNotFound(msg: String, cause: Throwable? = null) : GetArticleResponseByKeyException(
        msg,
        "OWNER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Owner not found.",
        cause
    )

    class NotAuthorized(msg: String, cause: Throwable? = null) : GetArticleResponseByKeyException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class ContentNotFound(msg: String, cause: Throwable? = null): GetArticleResponseByKeyException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : GetArticleResponseByKeyException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: CreateFullArticleResponseException): GetArticleResponseByKeyException = when (ex) {
            is CreateFullArticleResponseException.NoTranslations -> NoTranslations(ex.message, ex.cause)
            is CreateFullArticleResponseException.File -> File(ex.message, ex.cause)
            is CreateFullArticleResponseException.OwnerNotFound -> OwnerNotFound(ex.message, ex.cause)
            is CreateFullArticleResponseException.Database -> Database(ex.message, ex.cause)
        }

        fun from(ex: FindContentAuthorizedException): GetArticleResponseByKeyException {
            return when (ex) {
                is FindContentAuthorizedException.Database -> Database(ex.message, ex.cause)
                is FindContentAuthorizedException.NotFound -> ContentNotFound(ex.message, ex.cause)
                is FindContentAuthorizedException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
            }
        }
    }
}