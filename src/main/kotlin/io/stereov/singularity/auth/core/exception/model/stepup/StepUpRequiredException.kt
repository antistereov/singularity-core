package io.stereov.singularity.auth.core.exception.model.stepup

import io.stereov.singularity.auth.core.exception.AuthenticationException

class StepUpRequiredException(
    msg: String,
    cause: Throwable? = null
) : AuthenticationException(msg, CODE, cause) {

    companion object {
        const val CODE = "STEP_UP_REQUIRED"
    }
}