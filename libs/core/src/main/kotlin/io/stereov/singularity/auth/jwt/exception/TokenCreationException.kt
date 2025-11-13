package io.stereov.singularity.auth.jwt.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class TokenCreationException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class Encoding(msg: String, cause: Throwable? = null) : TokenCreationException(msg, CODE, cause) {
        companion object {
            const val CODE = "TOKEN_ENCODING_FAILED"
        }
    }
}