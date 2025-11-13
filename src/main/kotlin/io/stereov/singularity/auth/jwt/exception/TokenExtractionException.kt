package io.stereov.singularity.auth.jwt.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class TokenExtractionException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class Invalid(msg: String, cause: Throwable? = null) : TokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "INVALID_TOKEN"
        }
    }
    class Expired(msg: String, cause: Throwable? = null) : TokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "TOKEN_EXPIRED"
        }
    }
    class Missing(msg: String, cause: Throwable? = null) : TokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "TOKEN_MISSING"
        }
    }
}