package io.stereov.singularity.core.group.exception

import io.stereov.singularity.core.global.exception.BaseWebException

open class GroupException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
