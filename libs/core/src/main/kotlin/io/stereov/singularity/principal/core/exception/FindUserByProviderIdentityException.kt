package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.encryption.exception.DatabaseEncryptionFailure
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a base exception for errors encountered while searching for a user
 * using a provided identity.
 *
 * This sealed class extends [SingularityException] to provide structured error
 * details such as a message, error code, HTTP status, a detailed description, and
 * an optional underlying cause. It is specifically designed to handle various
 * scenarios that may occur during the process of identifying a user by their
 * provider identity.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of user-related error.
 * @param status The corresponding HTTP status indicating the nature of the error.
 * @param description A detailed description of the error providing additional context.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class FindUserByProviderIdentityException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing a scenario where a user with the specified email
     * could not be found.
     *
     * This exception is a specific type of [FindUserByProviderIdentityException].
     * It indicates that no user exists in the system with the given email, which
     * may occur during operations related to identifying a user by their provider identity.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see UserNotFoundFailure
     */
    class NotFound(msg: String, cause: Throwable? = null) : FindUserByProviderIdentityException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception representing a failure during hash generation or verification.
     *
     * This exception is a specific type of [FindUserByProviderIdentityException].
     * It indicates that an error occurred while generating or verifying a hash,
     * typically during operations related to identifying a user by their provider identity.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see HashFailure
     */
    class Hash(msg: String, cause: Throwable? = null) : FindUserByProviderIdentityException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception that occurs while retrieving a user from the database.
     *
     * This exception is thrown when there is a failure in retrieving user information
     * from the database. It extends `FindUserByProviderIdentityException` to provide
     * additional context for database-related failures.
     *
     * @param msg A message describing the error encountered during the database operation.
     * @param cause The underlying cause of the failure, if available.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : FindUserByProviderIdentityException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception that occurs during encryption or decryption of user data.
     *
     * This exception is a subtype of `FindUserByProviderIdentityException` and is used to
     * indicate a failure in encrypting or decrypting sensitive user information. It includes
     * a descriptive message, a unique error code, an HTTP status, and an optional root cause
     * for debugging purposes.
     *
     * @param msg The error message providing context for the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see DatabaseEncryptionFailure
     */
    class Encryption(msg: String, cause: Throwable? = null) : FindUserByProviderIdentityException(
        msg,
        DatabaseEncryptionFailure.CODE,
        DatabaseEncryptionFailure.STATUS,
        DatabaseEncryptionFailure.DESCRIPTION,
        cause
    )
}
