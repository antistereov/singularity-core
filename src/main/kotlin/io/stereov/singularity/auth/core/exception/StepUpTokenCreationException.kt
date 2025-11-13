package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.global.exception.SingularityException

sealed class StepUpTokenCreationException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class Forbidden(msg: String, cause: Throwable? = null) : StepUpTokenCreationException(msg, CODE, cause) {
        companion object {
            const val CODE = "STEP_UP_TOKEN_CREATION_FORBIDDEN"
        }
    }
    class Encoding(msg: String, cause: Throwable? = null) : StepUpTokenCreationException(msg, CODE, cause) {
        companion object {
            const val CODE = TokenCreationException.Encoding.CODE
        }
    }
}