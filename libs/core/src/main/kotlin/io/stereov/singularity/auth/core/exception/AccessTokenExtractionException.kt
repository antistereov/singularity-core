package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class AccessTokenExtractionException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class Invalid(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "INVALID_ACCESS_TOKEN"
        }
    }
    class Expired(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "ACCESS_TOKEN_EXPIRED"
        }
    }
    class Missing(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "ACCESS_TOKEN_MISSING"
        }
    }
    class Cache(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "ACCESS_TOKEN_ALLOWLIST_READING_FAILURE"
        }
    }
}