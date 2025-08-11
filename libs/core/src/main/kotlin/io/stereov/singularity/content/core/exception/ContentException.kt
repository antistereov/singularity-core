package io.stereov.singularity.content.core.exception

import io.stereov.singularity.global.exception.BaseWebException

open class ContentException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
