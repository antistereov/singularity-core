package io.stereov.singularity.auth.jwt.exception.model

import io.stereov.singularity.global.exception.SingularityException

sealed class TokenException(
    message: String,
    code: String,
    cause: Throwable? = null
) : SingularityException(message, code, cause) {

    class Invalid(msg: String, cause: Throwable? = null) : TokenException(msg, "INVALID_TOKEN", cause)
    class Expired(msg: String, cause: Throwable? = null) : TokenException(msg, "TOKEN_EXPIRED", cause)
    class Missing(msg: String, cause: Throwable? = null) : TokenException(msg, "TOKEN_MISSING", cause)
}