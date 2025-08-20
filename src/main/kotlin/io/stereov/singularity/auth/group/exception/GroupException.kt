package io.stereov.singularity.auth.group.exception

import io.stereov.singularity.global.exception.BaseWebException

open class GroupException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
