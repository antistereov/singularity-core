package io.stereov.singularity.auth.jwt.exception.model

import io.stereov.singularity.global.exception.SingularityException

sealed class TokenCreationException(
    msg: String,
    code: String,
    cause: Throwable? = null
) : SingularityException(msg, code, cause) {

    class Encoding(msg: String, cause: Throwable? = null) : TokenCreationException(msg, "JWT_ENCODING_FAILED", cause)
    class Forbidden(msg: String, cause: Throwable? = null) : TokenCreationException(msg, "TOKEN_CREATION_FORBIDDEN", cause)
}