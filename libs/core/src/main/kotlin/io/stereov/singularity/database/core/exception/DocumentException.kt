package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DocumentException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates an invalid document stored in the database.
     *
     * This exception extends [DocumentException].
     *
     * @param msg The error message providing details about the specific failure.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `INVALID_DATABASE_OBJECT`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Invalid(msg: String, cause: Throwable? = null) : DocumentException(
        msg,
        "INVALID_DATABASE_OBJECT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Indicates an invalid document stored in the database.",
        cause
    )
}
