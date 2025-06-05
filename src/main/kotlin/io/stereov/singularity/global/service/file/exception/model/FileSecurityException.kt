package io.stereov.singularity.global.service.file.exception.model

import io.stereov.singularity.global.service.file.exception.FileException

class FileSecurityException(msg: String, cause: Throwable? = null) : FileException(msg, cause)
