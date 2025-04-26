package io.stereov.web.global.exception.model

import io.stereov.web.global.exception.GlobalBaseWebException

class InvalidDocumentException(
    message: String,
    cause: Throwable? = null,
) : GlobalBaseWebException(
    msg = message, cause
)
