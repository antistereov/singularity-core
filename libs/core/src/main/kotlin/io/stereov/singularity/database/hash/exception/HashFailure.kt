package io.stereov.singularity.database.hash.exception

import io.stereov.singularity.database.hash.exception.HashFailure.CODE
import io.stereov.singularity.database.hash.exception.HashFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a general failure associated with hashing operations.
 *
 * This object contains a predefined error code, description, and HTTP status
 * to signify a broad, non-specific issue during hashing processes.
 *
 * @property CODE `HASH_FAILURE`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object HashFailure {
    const val CODE = "HASH_FAILURE"
    const val DESCRIPTION = "Exception representing a general failure related to hashing operations."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}