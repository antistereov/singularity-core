package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.token.model.PasswordResetToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a hierarchy of exceptions specifically related to the extraction of password reset tokens.
 *
 * This sealed class is a specialization of [SingularityException], providing detailed error codes,
 * HTTP statuses, and additional context for scenarios where extraction of password reset tokens fails.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure during token extraction.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class PasswordResetTokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the [PasswordResetToken] cannot be decoded.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `PASSWORD_RESET_TOKEN_INVALID`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Invalid
     */
    class Invalid(msg: String, cause: Throwable? = null) : PasswordResetTokenExtractionException(
        msg,
        CODE,
        STATUS,
        DESCRIPTION,
        cause
    ) {

        companion object {
            const val CODE = "PASSWORD_RESET_TOKEN_INVALID"
            const val DESCRIPTION = "Indicates that the password reset token cannot be decoded."
            val STATUS = HttpStatus.UNAUTHORIZED
        }
    }

    /**
     * Indicates that the [PasswordResetToken] is expired.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `PASSWORD_RESET_TOKEN_EXPIRED`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Expired
     */
    class Expired(msg: String, cause: Throwable? = null) : PasswordResetTokenExtractionException(
        msg,
        "PASSWORD_RESET_TOKEN_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the password reset token is expired.",
        cause
    )

    /**
     * Thrown when the [PasswordResetToken] is missing.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `PASSWORD_RESET_TOKEN_MISSING`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenExtractionException.Missing
     */
    class Missing(msg: String, cause: Throwable? = null) : PasswordResetTokenExtractionException(
        msg,
        "PASSWORD_RESET_TOKEN_MISSING",
        HttpStatus.UNAUTHORIZED,
        "Thrown when the password reset token is missing.",
        cause
    )

    /**
     * Represents an exception occurring during the decryption of the secret
     * stored within a [PasswordResetToken].
     *
     * Extends [PasswordResetTokenExtractionException].
     *
     * @param msg The error message describing the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `PASSWORD_RESET_TOKEN_SECRET_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Secret(msg: String, cause: Throwable? = null) : PasswordResetTokenExtractionException(
        msg,
        "PASSWORD_RESET_TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an exception occurs when trying to decrypt the secret stored inside the password reset token.",
        cause
    )

    companion object {

        /**
         * Converts a [TokenExtractionException] into an equivalent [PasswordResetTokenExtractionException].
         *
         * This method maps specific subclasses of [TokenExtractionException] to the corresponding
         * exception types in the context of password reset token handling.
         *
         * @param ex The instance of [TokenExtractionException] to be converted.
         * @return An instance of [PasswordResetTokenExtractionException] that reflects the type and details of the input exception.
         */
        fun fromTokenExtractionException(ex: TokenExtractionException): PasswordResetTokenExtractionException {
            return when (ex) {
                is TokenExtractionException.Expired -> Expired(ex.message, ex.cause)
                is TokenExtractionException.Invalid -> Invalid(ex.message, ex.cause)
                is TokenExtractionException.Missing -> Missing(ex.message, ex.cause)
            }
        }
    }

}
