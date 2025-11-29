package io.stereov.singularity.database.encryption.exception

import io.stereov.singularity.database.encryption.exception.DatabaseEncryptionFailure.CODE
import io.stereov.singularity.database.encryption.exception.DatabaseEncryptionFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Holds constant values representing a database encryption failure.
 *
 * This object defines the error code, description, and HTTP status that describe
 * a general failure related to database encryption operations. It is intended to be
 * used as a standardized representation of this specific error across the application.
 *
 * @property CODE `DATABASE_ENCRYPTION_FAILURE`.
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR].
 */
object DatabaseEncryptionFailure {

    const val CODE = "DATABASE_ENCRYPTION_FAILURE"
    const val DESCRIPTION = "Exception representing a general failure related to database encryption operations."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}