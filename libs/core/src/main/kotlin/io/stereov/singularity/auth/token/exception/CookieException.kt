package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class CookieException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Thrown when an exception occurred during the creation of a cookie.
     *
     * Extends [CookieException].
     *
     * @param msg The error message providing details about the missing entity.
     * @param cause The root cause of this exception, if any.
     *
     * @property code `COOKIE_CREATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Creation(msg: String, cause: Throwable? = null) : CookieException(
        msg,
        "COOKIE_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an exception occurred during the creation of a cookie.",
        cause
    )
}