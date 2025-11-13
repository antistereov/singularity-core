package io.stereov.singularity.file.core.exception

import io.stereov.singularity.global.exception.SingularityException

open class FileException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
