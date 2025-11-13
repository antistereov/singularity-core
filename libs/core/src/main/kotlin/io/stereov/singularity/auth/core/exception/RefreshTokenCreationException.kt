package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.global.exception.SingularityException

sealed class RefreshTokenCreationException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class Failed(msg: String, cause: Throwable? = null) : AccessTokenCreationException(msg, CODE, cause) {
        companion object {
            const val CODE = "REFRESH_TOKEN_CREATION_FAILED"
        }
    }
    class Encoding(msg: String, cause: Throwable? = null) : AccessTokenCreationException(msg, CODE, cause) {
        companion object {
            const val CODE = TokenCreationException.Encoding.CODE
        }
    }
}