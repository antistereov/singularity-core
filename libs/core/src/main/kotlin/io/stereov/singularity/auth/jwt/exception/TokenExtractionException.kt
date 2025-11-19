package io.stereov.singularity.auth.jwt.exception

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException.Expired.Companion.CODE
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException.Invalid.Companion.CODE
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException.Missing.Companion.CODE
import io.stereov.singularity.global.exception.SingularityException

/**
 * Represents exceptions that occur during the token extraction process.
 *
 * This sealed class serves as a base for specific exceptions related to token extraction issues,
 * such as invalid, expired, or missing tokens. Each subclass provides a precise context
 * for particular failure scenarios by associating an error code and an optional cause.
 *
 * @param msg The error message describing the failure.
 * @param code The error code associated with the failure.
 * @param cause The underlying exception causing the failure, if any.
 */
sealed class TokenExtractionException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Represents an exception indicating an invalid token during the token extraction process.
     *
     * This exception is a subclass of [TokenExtractionException] and is used
     * to provide detailed context when a token is determined to be invalid.
     * It associates a predefined error code ([CODE]) that specifies the nature of the failure.
     *
     * @property CODE The error code representing an invalid token.
     * @param msg The error message describing the invalid token.
     * @param cause The underlying exception that caused the invalid token error, if any.
     */
    class Invalid(msg: String, cause: Throwable? = null) : TokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "INVALID_TOKEN"
        }
    }

    /**
     * Represents an exception indicating that a token has expired during the token extraction process.
     *
     * This exception is a subclass of [TokenExtractionException] and is used to provide
     * detailed context when a token is determined to no longer be valid because it has
     * surpassed its expiration time. It associates a predefined error code ([CODE])
     * to specify the nature of the failure.
     *
     * @param msg The error message describing the expired token issue.
     * @param cause The underlying exception that caused the expired token issue, if any.
     */
    class Expired(msg: String, cause: Throwable? = null) : TokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "TOKEN_EXPIRED"
        }
    }

    /**
     * Represents an exception indicating that a token is missing during the token extraction process.
     *
     * This exception is a subclass of [TokenExtractionException] and is used to provide
     * detailed context when a required token is determined to be absent. It associates
     * a predefined error code ([CODE]) to specify the nature of the failure.
     *
     * @param msg The error message describing the missing token.
     * @param cause The underlying exception that caused the missing token error, if any.
     */
    class Missing(msg: String, cause: Throwable? = null) : TokenExtractionException(msg, CODE, cause) {
        companion object {
            const val CODE = "TOKEN_MISSING"
        }
    }
}