package io.stereov.singularity.auth.group.exception

import io.stereov.singularity.global.exception.SingularityException

open class GroupException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
