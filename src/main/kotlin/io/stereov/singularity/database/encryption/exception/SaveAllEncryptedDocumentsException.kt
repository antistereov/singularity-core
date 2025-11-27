package io.stereov.singularity.database.encryption.exception

import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specific to encrypted database operations.
 *
 * This is a sealed class extending [SingularityException], serving as the base class
 * for all exceptions related to encrypted database processes and operations.
 *
 * @param msg The error message describing the issue encountered.
 * @param code The error code representing this particular exception.
 * @param status The HTTP status associated with the exception.
 * @param description A description providing additional context about the exception.
 * @param cause The underlying cause of the exception, if applicable.
 *
 * @see DatabaseException
 */
sealed class SaveAllEncryptedDocumentsException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {


    /**
     * Exception thrown when an encrypted database operation fails.
     *
     * This exception is a subclass of [SaveAllEncryptedDocumentsException].
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of this exception, if any.
     *
     * @property code `DATABASE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see DatabaseException.Database
     */
    class Database(msg: String, cause: Throwable? = null) : SaveAllEncryptedDocumentsException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    /**
     * Exception representing a failure related to database encryption operations.
     *
     * This exception is a subclass of [SaveAllEncryptedDocumentsException].
     *
     * @param msg The error message describing the encryption failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `DATABASE_ENCRYPTION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Encryption(msg: String, cause: Throwable? = null) : SaveAllEncryptedDocumentsException(
        msg,
        "DATABASE_ENCRYPTION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a failure related to database encryption operations.",
        cause
    )

    /**
     * Exception thrown when a post-commit side effect fails after a database operation has been successfully committed.
     *
     * This exception is a subclass of [SaveAllEncryptedDocumentsException].
     *
     * @param msg The error message describing the specific side effect failure.
     * @param cause The underlying cause of this exception, if available.
     *
     * @property code `POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE`
     * @property status [HttpStatus.MULTI_STATUS]
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : SaveAllEncryptedDocumentsException(
        msg,
        "POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a post-commit side effect fails after a database operation has been successfully committed.",
        cause
    )
}
