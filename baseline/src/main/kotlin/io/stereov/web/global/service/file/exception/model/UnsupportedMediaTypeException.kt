package io.stereov.web.global.service.file.exception.model

import io.stereov.web.global.service.file.exception.FileException

class UnsupportedMediaTypeException(msg: String, cause: Throwable? = null) : FileException(msg, cause)
