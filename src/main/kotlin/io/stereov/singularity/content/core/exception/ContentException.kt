package io.stereov.singularity.content.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ContentException(
    msg: String, 
    code: String, 
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the content is not accessible by the user that performs the request.
     * Extends [ContentException].
     *
     * @param msg The error message.
     * @param cause The cause.
     *
     * @see NotAuthorizedFailure
     */
    class NotAuthorized(msg: String, cause: Throwable? = null) : ContentException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )
}
