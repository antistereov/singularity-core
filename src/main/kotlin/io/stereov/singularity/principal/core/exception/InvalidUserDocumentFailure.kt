package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure.CODE
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Provides constants for handling invalid user document errors.
 *
 * This object encapsulates metadata related to an invalid user document.
 * It includes an error code, a description, and an associated HTTP status.
 * These constants can be used for error handling and communication purposes
 * when a requested user document is stored in an invalid format.
 *
 * @property CODE `INVALID_USER_DOCUMENT`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object InvalidUserDocumentFailure {
    const val CODE = "INVALID_USER_DOCUMENT"
    const val DESCRIPTION = "A requested user document was stored in an invalid format."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}