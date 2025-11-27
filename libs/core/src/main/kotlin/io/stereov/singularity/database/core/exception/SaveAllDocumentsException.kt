package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class SaveAllDocumentsException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a database-related failure during the saving of all documents.
     *
     * This exception extends [SaveAllDocumentsException] and is specifically used to
     * indicate general failures that occur within the database system during the operation
     * of saving all documents.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of the exception, if available.
     *
     * @property code `DATABASE_SAVING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : SaveAllDocumentsException(
        msg,
        "DATABASE_SAVING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a general failure related to database saving operations.",
        cause
    )
}
