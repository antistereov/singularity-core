package io.stereov.singularity.core.global.exception.model

import io.stereov.singularity.core.global.exception.GlobalBaseWebException

class InvalidDocumentException(
    message: String,
    cause: Throwable? = null,
) : GlobalBaseWebException(
    msg = message, cause
)
