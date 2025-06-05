package io.stereov.singularity.file.exception.model

import io.stereov.singularity.file.exception.FileException

class DeleteFailedException(msg: String, cause: Throwable? = null) : FileException(msg, cause)
