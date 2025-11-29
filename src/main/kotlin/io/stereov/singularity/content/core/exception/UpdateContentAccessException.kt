package io.stereov.singularity.content.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.ResponseMappingFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.UserNotFoundFailure
import org.springframework.http.HttpStatus

sealed class UpdateContentAccessException(
    msg: String, 
    code: String, 
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class NotAuthorized(msg: String, cause: Throwable? = null) : UpdateContentAccessException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class ContentNotFound(msg: String, cause: Throwable? = null): UpdateContentAccessException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : UpdateContentAccessException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class GroupNotFound(msg: String, cause: Throwable? = null) : UpdateContentAccessException(
        msg,
        "GROUP_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Group not found.",
        cause
    )

    class UserNotFound(msg: String, cause: Throwable? = null) : UpdateContentAccessException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    class ResponseMapping(msg: String, cause: Throwable? = null) : UpdateContentAccessException(
        msg,
        ResponseMappingFailure.CODE,
        ResponseMappingFailure.STATUS,
        ResponseMappingFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: FindContentAuthorizedException): UpdateContentAccessException {
            return when (ex) {
                is FindContentAuthorizedException.Database -> Database(ex.message, ex.cause)
                is FindContentAuthorizedException.NotFound -> ContentNotFound(ex.message, ex.cause)
                is FindContentAuthorizedException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
            }
        }
    }
}
