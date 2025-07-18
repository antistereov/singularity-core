package io.stereov.singularity.content.core.tag.exception

import io.stereov.singularity.global.exception.BaseWebException

open class TagException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
