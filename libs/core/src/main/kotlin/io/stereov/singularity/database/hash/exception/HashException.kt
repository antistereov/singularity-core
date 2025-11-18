package io.stereov.singularity.database.hash.exception

import io.stereov.singularity.global.exception.SingularityException

/**
 * Represents exceptions related to hashing operations.
 *
 * This sealed class serves as a base for specific hashing-related exception types.
 *
 * @param msg The error message describing the exception.
 * @param code The error code associated with the exception.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class HashException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Represents an exception that occurs when there is a failure related to hash secrets.
     *
     * This exception is used to indicate issues that arise during operations involving
     * hash secrets, such as retrieving the current hash secret or decoding it.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     */
    class Secret(msg: String, cause: Throwable? = null): HashException(msg, CODE, cause) {
        companion object { const val CODE = "HASH_SECRET_FAILURE" }
    }

    /**
     * Represents an exception that occurs during encoding operations in the hashing process.
     *
     * This exception is typically used to indicate issues related to encoding, such as
     * errors decoding hash secrets or encoding generated hash values.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     */
    class Encoding(msg: String, cause: Throwable? = null): HashException(msg, CODE, cause) {
        companion object { const val CODE = "HASH_ENCODING_FAILURE" }
    }

    /**
     * Represents an exception that occurs during the hashing process.
     *
     * This exception is used to signify failures related to hashing operations, for example,
     * when initializing a hashing algorithm, validating hashes, or performing other hashing-related tasks.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     */
    class Hashing(msg: String, cause: Throwable? = null): HashException(msg, CODE, cause) {
        companion object { const val CODE = "HASHING_FAILURE" }
    }
}