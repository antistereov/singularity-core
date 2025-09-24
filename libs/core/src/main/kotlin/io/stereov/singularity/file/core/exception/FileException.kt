package io.stereov.singularity.file.core.exception

import io.stereov.singularity.global.exception.BaseWebException

open class FileException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
