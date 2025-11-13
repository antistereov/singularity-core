package io.stereov.singularity.admin.core.exception

import io.stereov.singularity.global.exception.SingularityException

open class AdminException(
    msg: String,
    code: Strng,
    cause: Throwable? = null
) : SingularityException(msg, code, cause)