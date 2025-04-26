package io.stereov.singularity.global.exception.model

import io.stereov.singularity.global.exception.GlobalBaseWebException

class DocumentNotFoundException(msg: String, cause: Throwable? = null) : GlobalBaseWebException(msg, cause)
