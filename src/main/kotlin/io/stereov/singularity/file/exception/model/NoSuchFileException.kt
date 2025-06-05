package io.stereov.singularity.file.exception.model

import io.stereov.singularity.file.exception.FileException

class NoSuchFileException(msg: String, cause: Throwable? = null) : io.stereov.singularity.file.exception.FileException(msg,cause)
