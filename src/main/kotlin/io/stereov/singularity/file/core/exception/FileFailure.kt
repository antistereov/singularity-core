package io.stereov.singularity.file.core.exception

import io.stereov.singularity.file.core.exception.FileFailure.CODE
import io.stereov.singularity.file.core.exception.FileFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Defines a constant representation of a general failure related to file operations.
 *
 * This object is used to represent scenarios where file-related operations encounter
 * an unexpected failure. It provides standard values like the error code, description,
 * and an associated HTTP status to maintain consistency in error handling and responses.
 *
 * @property CODE `FILE_FAILURE`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object FileFailure {
    const val CODE = "FILE_FAILURE"
    const val DESCRIPTION = "Exception representing a general failure related to file operations."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}