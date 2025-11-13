package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class StepUpTokenException(
    msg: String,
    code: String,
    cause: Throwable? = null
) : SingularityException(msg, code, cause) {

    class Invalid(msg: String, cause: Throwable? = null) : StepUpTokenException(
        msg,
        "INVALID_STEP_UP_TOKEN",
        cause
    )

    class Expired(cause: Throwable? = null) : StepUpTokenException(
        "Access token is expired${cause?.let { ": ${it.message}" } }",
        "STEP_UP_TOKEN_EXPIRED",
        cause
    )

    class Missing(cause: Throwable? = null) : StepUpTokenException(
        "Access token is missing${cause?.let { ": ${it.message}" } }",
        "STEP_UP_TOKEN_MISSING",
        cause
    )
}