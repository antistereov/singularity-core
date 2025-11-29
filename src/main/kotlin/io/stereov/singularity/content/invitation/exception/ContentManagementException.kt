package io.stereov.singularity.content.invitation.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ContentManagementException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class ContentTypeNotFound(msg: String, cause: Throwable? = null) : ContentManagementException(
        msg,
        "CONTENT_TYPE_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Content type not found.",
        cause
    )
}
