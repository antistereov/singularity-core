package io.stereov.singularity.database.encryption.exception

import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specific to encrypted database operations.
 *
 * This sealed class extends [SingularityException] and provides a hierarchy for
 * handling errors related to operations on encrypted databases. It includes multiple
 * specific exception types to handle different failure scenarios that might occur
 * during encrypted database interactions.
 *
 * @param msg A message describing the error.
 * @param code The associated error code.
 * @param cause The underlying cause of this exception, if available.
 */
sealed class EncryptedDatabaseException(
    msg: String,
    code: String,
    status: HttpStatus,
    cause: Throwable?
) : SingularityException(msg, code, status, cause) {

    /**
     * Exception thrown when an entity is not found in the encrypted database.
     *
     * This class represents errors occurring due to the absence of a requested
     * entity in the database during encrypted operations.
     *
     * @param msg A detailed message describing the missing entity and its context.
     * @param cause The underlying exception, if available, that caused this error.
     */
    class NotFound(msg: String, cause: Throwable? = null): EncryptedDatabaseException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = DatabaseException.NotFound.CODE
            val STATUS = DatabaseException.NotFound.STATUS
        }
    }

    /**
     * Exception thrown when an encrypted database operation fails.
     *
     * This class extends [EncryptedDatabaseException] and represents errors
     * specifically related to encrypted database functionalities. It provides
     * a mechanism for handling failures that occur during encrypted database
     * operations, such as retrieval, existence checks, or modifications,
     * while carrying additional context about the failure.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of this exception, if any.
     */
    class Database(msg: String, cause: Throwable? = null) : EncryptedDatabaseException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = DatabaseException.Database.CODE
            val STATUS = DatabaseException.Database.STATUS
        }
    }

    /**
     * Exception representing a failure related to database encryption operations.
     *
     * This exception is a specific type of [EncryptedDatabaseException] used to indicate
     * an error that occurs during encryption or decryption processes within the context of
     * database operations. It is primarily utilized to signal encryption-related issues that
     * can prevent data from being adequately stored or retrieved securely.
     *
     * @param msg The error message describing the encryption failure.
     * @param cause The underlying cause of the exception, if available.
     */
    class Encryption(msg: String, cause: Throwable? = null) : EncryptedDatabaseException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "DATABASE_ENCRYPTION_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    /**
     * Exception thrown when a post-commit side effect fails after a database operation has been successfully committed.
     *
     * This exception is a specialized type of [EncryptedDatabaseException] used to indicate that,
     * although the main database transaction succeeded, an error occurred during a subsequent
     * operation such as decryption, cache update, or event publishing. It allows distinguishing
     * errors that occur post-commit from errors during the primary database operation.
     *
     * @param msg The error message describing the specific side effect failure.
     * @param cause The underlying cause of this exception, if available.
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : EncryptedDatabaseException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = DatabaseException.PostCommitSideEffect.CODE
            val STATUS = DatabaseException.PostCommitSideEffect.STATUS
        }
    }
}
