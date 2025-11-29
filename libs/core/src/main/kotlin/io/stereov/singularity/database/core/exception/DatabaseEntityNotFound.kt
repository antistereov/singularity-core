package io.stereov.singularity.database.core.exception

import io.stereov.singularity.database.core.exception.DatabaseEntityNotFound.CODE
import io.stereov.singularity.database.core.exception.DatabaseEntityNotFound.STATUS
import org.springframework.http.HttpStatus

/**
 * Static definition representing an error when a database entity is not found.
 *
 * This object provides a standard reference for defining the error code, description,
 * and associated HTTP status for scenarios where a specific entity is missing in the database.
 *
 * @property CODE `DATABASE_ENTITY_NOT_FOUND`.
 * @property STATUS [HttpStatus.NOT_FOUND]
 */
object DatabaseEntityNotFound {
    const val CODE = "DATABASE_ENTITY_NOT_FOUND"
    const val DESCRIPTION = "Exception representing a failure to find a database entity."
    val STATUS = HttpStatus.NOT_FOUND
}