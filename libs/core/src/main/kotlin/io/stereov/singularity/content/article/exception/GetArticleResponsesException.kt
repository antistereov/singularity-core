package io.stereov.singularity.content.article.exception

import io.stereov.singularity.content.core.exception.NotAuthorizedFailure
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.FindAllDocumentsPaginatedException
import io.stereov.singularity.file.core.exception.FileFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.translate.exception.TranslateException
import org.springframework.http.HttpStatus

sealed class GetArticleResponsesException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class NoTranslations(msg: String, cause: Throwable? = null) : GetArticleResponsesException(
        msg,
        TranslateException.NoTranslations.CODE,
        TranslateException.NoTranslations.STATUS,
        TranslateException.NoTranslations.DESCRIPTION,
        cause
    )

    class File(msg: String, cause: Throwable? = null) : GetArticleResponsesException(
        msg,
        FileFailure.CODE,
        FileFailure.STATUS,
        FileFailure.DESCRIPTION,
        cause
    )

    class NotAuthorized(msg: String, cause: Throwable? = null) : GetArticleResponsesException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : GetArticleResponsesException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: CreateFullArticleResponseException): GetArticleResponsesException = when (ex) {
            is CreateFullArticleResponseException.NoTranslations -> NoTranslations(ex.message, ex.cause)
            is CreateFullArticleResponseException.Database -> Database(ex.message, ex.cause)
        }

        fun from(ex: FindAllDocumentsPaginatedException): GetArticleResponsesException {
            return when (ex) {
                is FindAllDocumentsPaginatedException.Database -> Database(ex.message, ex.cause)
            }
        }
    }
}