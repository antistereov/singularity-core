package io.stereov.singularity.email.core.exception

import io.stereov.singularity.global.exception.SingularityException

open class EmailException(message: String, cause: Throwable? = null) : SingularityException(message, cause)
