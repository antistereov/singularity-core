package io.stereov.singularity.content.article.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.file.core.exception.FileFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.translate.exception.TranslateException
import org.springframework.http.HttpStatus

sealed class CreateFullArticleResponseException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : CreateFullArticleResponseException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NoTranslations(msg: String, cause: Throwable? = null) : CreateFullArticleResponseException(
        msg,
        TranslateException.NoTranslations.CODE,
        TranslateException.NoTranslations.STATUS,
        TranslateException.NoTranslations.DESCRIPTION,
        cause
    )

    class File(msg: String, cause: Throwable? = null) : CreateFullArticleResponseException(
        msg,
        FileFailure.CODE,
        FileFailure.STATUS,
        FileFailure.DESCRIPTION,
        cause
    )

    class OwnerNotFound(msg: String, cause: Throwable? = null) : CreateFullArticleResponseException(
        msg,
        "OWNER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Owner not found.",
        cause
    )
}