package io.stereov.singularity.content.core.exception

import io.stereov.singularity.global.exception.SingularityException

open class ContentException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
