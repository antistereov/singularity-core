package io.stereov.singularity.admin.core.exception

import io.stereov.singularity.global.exception.BaseWebException

open class AdminException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)