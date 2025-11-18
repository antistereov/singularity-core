package io.stereov.singularity.database.encryption.exception

import io.stereov.singularity.database.encryption.exception.EncryptionException.Cipher.Companion.CODE
import io.stereov.singularity.global.exception.SingularityException

/**
 * Represents exceptions related to encryption operations.
 *
 * This sealed class extends [SingularityException] and defines specific types of encryption-related
 * exceptions that can occur during various encryption and decryption processes. Each specialized
 * exception includes an associated error code and an optional cause to provide more context for the failure.
 *
 * @param msg A detailed message describing the encryption-related error.
 * @param code The associated error code representing the type of failure.
 * @param cause The underlying cause of this exception, if available.
 */
sealed class EncryptionException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Exception representing a failure related to object mapping during encryption operations.
     *
     * This exception is a specific type of [EncryptionException] used to indicate an error that
     * occurs when mapping objects as part of encryption or decryption processes fails.
     * It provides additional context regarding the mapping failure, making it easier to identify
     * and address the root cause of the problem.
     *
     * @param msg The error message describing the object mapping failure.
     * @param cause The underlying cause of this exception, if available.
     */
    class ObjectMapping(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_OBJECT_MAPPING_FAILURE" }
    }

    /**
     * Exception representing a failure related to encryption or decryption operations.
     *
     * This class extends [EncryptionException] and is used to indicate errors
     * that occur during encryption processes, such as issues with cipher initialization,
     * encryption, or decryption failures. It provides a specific error code
     * ([CODE]) to identify encryption-related failures distinctly.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of the exception, if available.
     */
    class Cipher(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_CIPHER_FAILURE" }
    }

    /**
     * Represents an exception indicating a failure related to encryption secrets.
     *
     * This exception is a specific type of [EncryptionException] used to signal errors
     * occurring during the processing of secrets, such as invalid
     * secret operations. It extends the encryption exception hierarchy by providing an
     * identifiable error code to classify these operations.
     *
     * @param msg The error message describing the failure.
     * @param cause The underlying cause of the exception, if available.
     */
    class Secret(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_SECRET_FAILURE" }
    }

    /**
     * Exception representing an encoding failure during encryption operations.
     *
     * This exception is a specific type of [EncryptionException] that indicates an error
     * occurring during the encoding process within encryption or decryption workflows.
     * It signifies that an encoding-related failure has prevented successful encryption
     * or decryption of data.
     *
     * @param msg The error message describing the encoding failure.
     * @param cause The underlying cause of the exception, if available.
     */
    class Encoding(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_ENCODING_FAILURE" }
    }
}