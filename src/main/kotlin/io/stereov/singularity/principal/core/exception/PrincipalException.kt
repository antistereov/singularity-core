package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to principal operations.
 *
 * This sealed class serves as a base for specific exceptions that may occur
 * when handling principal-related operations. It extends [SingularityException]
 * to provide a consistent structure for error details, including a message,
 * error code, HTTP status, detailed description, and optionally a root cause.
 *
 * @param msg The error message providing context for the exception.
 * @param code A unique code identifying the specific type of principal-related error.
 * @param status The corresponding HTTP status that represents this error.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class PrincipalException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing an invalid principal document error.
     *
     * This exception is thrown when a requested principal document is deemed invalid.
     * It extends the [PrincipalException].
     *
     * @param msg A message providing details about the invalid document.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see InvalidPrincipalDocumentFailure
     */
    class InvalidDocument(msg: String, cause: Throwable? = null) : PrincipalException(
        msg,
        InvalidPrincipalDocumentFailure.CODE,
        InvalidPrincipalDocumentFailure.STATUS,
        InvalidPrincipalDocumentFailure.DESCRIPTION,
        cause
    )
}
