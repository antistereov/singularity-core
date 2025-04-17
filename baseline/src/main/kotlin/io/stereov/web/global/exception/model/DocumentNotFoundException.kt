package io.stereov.web.global.exception.model

import io.stereov.web.global.exception.BaseWebException

class DocumentNotFoundException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
