package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.principal.core.exception.InvalidPrincipalDocumentFailure.CODE
import io.stereov.singularity.principal.core.exception.InvalidPrincipalDocumentFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Provides constants for handling invalid principal document errors.
 *
 * This object encapsulates metadata related to an invalid principal document.
 * It includes an error code, a description, and an associated HTTP status.
 * These constants can be used for error handling and communication purposes
 * when a requested principal document is stored in an invalid format.
 *
 * @property CODE `INVALID_PRINCIPAL_DOCUMENT`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object InvalidPrincipalDocumentFailure {
    const val CODE = "INVALID_PRINCIPAL_DOCUMENT"
    const val DESCRIPTION = "A requested principal document was stored in an invalid format."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}