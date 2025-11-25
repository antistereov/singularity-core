package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.token.model.EmailVerificationToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a hierarchy of exceptions specifically related to the extraction of email verification tokens.
 *
 * This sealed class is a specialization of [SingularityException], providing detailed error codes,
 * HTTP statuses, and additional context for scenarios where extraction of email verification tokens fails.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure during token extraction.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class EmailVerificationTokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the [EmailVerificationToken] cannot be decoded.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `INVALID_EMAIL_VERIFICATION_TOKEN`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Invalid
     */
    class Invalid(msg: String, cause: Throwable? = null) : EmailVerificationTokenExtractionException(
        msg,
        "EMAIL_VERIFICATION_TOKEN_INVALID",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the email verification token cannot be decoded.",
        cause
    )

    /**
     * Indicates that the [EmailVerificationToken] is expired.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `INVALID_EMAIL_VERIFICATION_TOKEN`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Expired
     */
    class Expired(msg: String, cause: Throwable? = null) : EmailVerificationTokenExtractionException(
        msg,
        "EMAIL_VERIFICATION_TOKEN_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the email verification token is expired.",
        cause
    )

    /**
     * Thrown when the [EmailVerificationToken] is missing.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `EMAIL_VERIFICATION_TOKEN_ALLOWLIST_READING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenExtractionException.Missing
     */
    class Missing(msg: String, cause: Throwable? = null) : EmailVerificationTokenExtractionException(
        msg,
        "EMAIL_VERIFICATION_TOKEN_MISSING",
        HttpStatus.UNAUTHORIZED,
        "Thrown when the email verification token is missing.",
        cause
    )

    companion object {

        /**
         * Converts a [TokenExtractionException] into an equivalent [EmailVerificationTokenExtractionException].
         *
         * This method maps specific subclasses of [TokenExtractionException] to the corresponding
         * exception types in the context of email verification token handling.
         *
         * @param ex The instance of [TokenExtractionException] to be converted.
         * @return An instance of [EmailVerificationTokenExtractionException] that reflects the type and details of the input exception.
         */
        fun fromTokenExtractionException(ex: TokenExtractionException): EmailVerificationTokenExtractionException {
            return when (ex) {
                is TokenExtractionException.Expired -> Expired(ex.message, ex.cause)
                is TokenExtractionException.Invalid -> Invalid(ex.message, ex.cause)
                is TokenExtractionException.Missing -> Missing(ex.message, ex.cause)
            }
        }
    }

}
