package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class AccessTokenException(
    msg: String,
    code: String,
    cause: Throwable? = null
) : SingularityException(msg, code, cause) {

    class Invalid(msg: String, cause: Throwable? = null) : AccessTokenException(
        msg,
        "INVALID_ACCESS_TOKEN",
        cause
    )

    class Expired(cause: Throwable? = null) : AccessTokenException(
        "Access token is expired${cause?.let { ": ${it.message}" } }",
        "ACCESS_TOKEN_EXPIRED",
        cause
    )

    class Missing(cause: Throwable? = null) : AccessTokenException(
        "Access token is missing${cause?.let { ": ${it.message}" } }",
        "ACCESS_TOKEN_MISSING",
        cause
    )
}