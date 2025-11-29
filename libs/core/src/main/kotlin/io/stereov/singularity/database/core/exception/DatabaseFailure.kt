package io.stereov.singularity.database.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure.CODE
import io.stereov.singularity.database.core.exception.DatabaseFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a static definition of a general database failure.
 *
 * The `DatabaseFailure` object is used as a standard reference
 * for defining the error code, description, and associated HTTP status
 * for general failures related to database operations.
 *
 * @property CODE `DATABASE_FAILURE`.
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object DatabaseFailure {
    const val CODE = "DATABASE_FAILURE"
    const val DESCRIPTION = "Exception representing a general failure related to database operations."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}