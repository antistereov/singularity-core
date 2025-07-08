package io.stereov.singularity.content.common.content.exception

import io.stereov.singularity.global.exception.BaseWebException

open class ContentException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
