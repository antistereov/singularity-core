package io.stereov.singularity.file.core.exception.model

import io.stereov.singularity.file.core.exception.FileException

class UnsupportedMediaTypeException(msg: String, cause: Throwable? = null) : FileException(msg, cause)
