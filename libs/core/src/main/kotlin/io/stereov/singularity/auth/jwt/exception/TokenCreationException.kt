package io.stereov.singularity.auth.jwt.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException.Encoding.Companion.CODE
import io.stereov.singularity.auth.jwt.exception.TokenCreationException.Secret.Companion.CODE
import io.stereov.singularity.global.exception.SingularityException

/**
 * Represents exceptions related to the creation of a JSON Web Token (JWT).
 *
 * This sealed class serves as a base for specific exceptions that may occur during
 * the token creation process, including issues with encoding or using the secret
 * required for signing the token. Each subclass provides a precise context for
 * particular failure scenarios by associating an error code and an optional cause.
 *
 * @param msg The error message describing the failure.
 * @param code The error code associated with the failure.
 * @param cause The underlying exception causing the failure, if any.
 */
sealed class TokenCreationException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Represents an exception that occurs during the encoding process of a JSON Web Token (JWT).
     *
     * This exception is a subclass of [TokenCreationException] and is used to provide detailed
     * context when encoding a JWT fails. Specifically, it associates a predefined error code
     * ([CODE]) that indicates the nature of the failure.
     *
     * @property CODE The error code representing a token encoding failure.
     * @param msg The error message describing the encoding failure.
     * @param cause The underlying exception that caused the encoding failure, if any.
     */
    class Encoding(msg: String, cause: Throwable? = null) : TokenCreationException(msg, CODE, cause) {
        companion object { const val CODE = "TOKEN_ENCODING_FAILED" }
    }

    /**
     * Represents an exception that occurs when there is a failure related to the secret
     * required for creating a JSON Web Token (JWT).
     *
     * This exception is a subclass of [TokenCreationException] and is specifically used
     * to indicate issues related to retrieving or using the secret necessary for
     * token encoding or signing operations.
     *
     * @property CODE The error code representing a token secret failure.
     * @param msg The error message describing the secret-related failure.
     * @param cause The underlying exception that caused the failure, if any.
     */
    class Secret(msg: String, cause: Throwable? = null) : TokenCreationException(msg, CODE, cause) {
        companion object { const val CODE = "TOKEN_SECRET_FAILURE" }
    }
}