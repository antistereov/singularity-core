package io.stereov.singularity.email.core.exception

import io.stereov.singularity.global.exception.BaseWebException

open class EmailException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
