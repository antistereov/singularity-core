package io.stereov.singularity.file.core.exception

import io.stereov.singularity.content.core.exception.NotAuthorizedFailure
import io.stereov.singularity.database.core.exception.DatabaseEntityNotFound
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.FindAllDocumentsPaginatedException
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.translate.exception.TranslateException
import org.springframework.http.HttpStatus

sealed class GetFilesException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class NoTranslations(msg: String, cause: Throwable? = null) : GetFilesException(
        msg,
        TranslateException.NoTranslations.CODE,
        TranslateException.NoTranslations.STATUS,
        TranslateException.NoTranslations.DESCRIPTION,
        cause
    )

    class NotFound(msg: String, cause: Throwable? = null) : GetFilesException(
        msg,
        DatabaseEntityNotFound.CODE,
        DatabaseEntityNotFound.STATUS,
        DatabaseEntityNotFound.DESCRIPTION,
        cause
    )

    class File(msg: String, cause: Throwable? = null) : GetFilesException(
        msg,
        FileFailure.CODE,
        FileFailure.STATUS,
        FileFailure.DESCRIPTION,
        cause
    )

    class NotAuthorized(msg: String, cause: Throwable? = null) : GetFilesException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : GetFilesException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: FileMetadataException): GetFilesException = when (ex) {
            is FileMetadataException.Database -> Database(ex.message, ex.cause)
            is FileMetadataException.NotFound -> NotFound(ex.message, ex.cause)
        }

        fun from(ex: FindAllDocumentsPaginatedException): GetFilesException {
            return when (ex) {
                is FindAllDocumentsPaginatedException.Database -> Database(ex.message, ex.cause)
            }
        }
    }
}