package io.stereov.singularity.content.article.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.InvalidDocumentFailure
import io.stereov.singularity.global.exception.InvalidRequestFailure
import io.stereov.singularity.global.exception.ResponseMappingFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class CreateArticleException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class InvalidRequest(msg: String, cause: Throwable? = null) : CreateArticleException(
        msg,
        InvalidRequestFailure.CODE,
        InvalidRequestFailure.STATUS,
        InvalidRequestFailure.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : CreateArticleException(
        msg,
        InvalidDocumentFailure.CODE,
        InvalidDocumentFailure.STATUS,
        InvalidDocumentFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : CreateArticleException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class ResponseMapping(msg: String, cause: Throwable? = null) : CreateArticleException(
        msg,
        ResponseMappingFailure.CODE,
        ResponseMappingFailure.STATUS,
        ResponseMappingFailure.DESCRIPTION,
        cause
    )

    companion object {
        fun from(ex: GetUniqueArticleKeyException): CreateArticleException {
            return when (ex) {
                is GetUniqueArticleKeyException.Database -> Database(ex.message, ex.cause)
                is GetUniqueArticleKeyException.InvalidDocument -> InvalidDocument(ex.message, ex.cause)
            }
        }
    }
}