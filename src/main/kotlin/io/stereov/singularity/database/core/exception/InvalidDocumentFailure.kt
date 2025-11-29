package io.stereov.singularity.database.core.exception

import org.springframework.http.HttpStatus

object InvalidDocumentFailure {
    const val CODE = "INVALID_DOCUMENT"
    const val DESCRIPTION = "Invalid document."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}