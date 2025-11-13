package io.stereov.singularity.global.exception

open class GlobalBaseWebException(
    msg: String,
    code: String,
    cause: Throwable? = null
) : SingularityException(msg, code, cause)
