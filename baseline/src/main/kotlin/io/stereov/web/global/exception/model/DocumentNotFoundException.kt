package io.stereov.web.global.exception.model

import io.stereov.web.global.exception.GlobalBaseWebException

class DocumentNotFoundException(msg: String, cause: Throwable? = null) : GlobalBaseWebException(msg, cause)
