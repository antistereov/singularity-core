package io.stereov.singularity.secrets.core.exception

import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.secrets.core.exception.SecretStoreException.KeyGenerator.Companion.CODE

/**
 * Represents exceptions related to operations in the secret store.
 * This is a sealed class extending [SingularityException], allowing for specific types of
 * exceptions related to secret store failures to be represented and handled distinctly.
 *
 * @constructor Creates a [SecretStoreException] with the given message, code, and cause.
 *
 * @param msg The error message describing the issue.
 * @param code The error code associated with this exception.
 * @param cause The cause of the exception, if any.
 */
sealed class SecretStoreException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Represents an exception that is thrown when a requested secret cannot be found in the secret store.
     *
     * @constructor Creates a [NotFound] exception with the specified message and optional cause.
     * @param msg The error message describing the missing secret.
     * @param cause The cause of the exception, if any.
     */
    class NotFound(msg: String, cause: Throwable? = null) : SecretStoreException(msg, CODE, cause) {
        companion object { const val CODE = "SECRET_NOT_FOUND" }
    }

    /**
     * Exception representing the failure of cryptographic key generation within the secret store operations.
     *
     * This exception is thrown when an error occurs while generating a cryptographic key
     * required for secret management. The associated [CODE] provides a specific error identifier
     * for such failures.
     *
     * @constructor Creates a [KeyGenerator] exception with the specified message and optional cause.
     *
     * @param msg The error message describing the key generation failure.
     * @param cause The cause of the exception, if any.
     */
    class KeyGenerator(msg: String, cause: Throwable? = null) : SecretStoreException(msg, CODE, cause) {
        companion object { const val CODE = "SECRET_KEY_GENERATION_FAILURE"}
    }

    /**
     * Represents an exception that occurs during a secret store operation.
     *
     * This exception is specifically used to indicate failures in performing
     * operations related to secrets, such as fetching or storing secrets in a
     * secret store or vault.
     *
     * @param msg The detail message for the exception.
     * @param cause The cause of the exception, which can be null.
     */
    class Operation(msg: String, cause: Throwable? = null) : SecretStoreException(msg, CODE, cause) {
        companion object { const val CODE = "SECRET_OPERATION_FAILURE"}
    }
}