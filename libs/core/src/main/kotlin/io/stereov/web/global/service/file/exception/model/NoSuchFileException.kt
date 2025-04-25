package io.stereov.web.global.service.file.exception.model

import io.stereov.web.global.service.file.exception.FileException

class NoSuchFileException(msg: String, cause: Throwable? = null) : FileException(msg,cause)
