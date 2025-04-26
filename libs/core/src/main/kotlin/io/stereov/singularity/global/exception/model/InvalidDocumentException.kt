package io.stereov.singularity.global.exception.model

import io.stereov.singularity.global.exception.GlobalBaseWebException

class InvalidDocumentException(
    message: String,
    cause: Throwable? = null,
) : GlobalBaseWebException(
    msg = message, cause
)
