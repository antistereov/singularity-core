package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.token.model.TotpSetupToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a hierarchy of exceptions specifically related to the extraction of TOTP setup tokens.
 *
 * This sealed class is a specialization of [SingularityException], providing detailed error codes,
 * HTTP statuses, and additional context for scenarios where extraction of TOTP setup tokens fails.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure during token extraction.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class TotpSetupTokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the [TotpSetupToken] cannot be decoded.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `TOTP_SETUP_TOKEN_INVALID`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Invalid
     */ 
    class Invalid(msg: String, cause: Throwable? = null) : TotpSetupTokenExtractionException(
        msg,
        "TOTP_SETUP_TOKEN_INVALID",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the TOTP setup token cannot be decoded.",
        cause
    )

    /**
     * Indicates that the [TotpSetupToken] is expired.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `TOTP_SETUP_TOKEN_EXPIRED`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Expired
     */
    class Expired(msg: String, cause: Throwable? = null) : TotpSetupTokenExtractionException(
        msg,
        "TOTP_SETUP_TOKEN_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the TOTP setup token is expired.",
        cause
    )

    /**
     * Thrown when the [TotpSetupToken] is missing.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `TOTP_SETUP_TOKEN_MISSING`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenExtractionException.Missing
     */
    class Missing(msg: String, cause: Throwable? = null) : TotpSetupTokenExtractionException(
        msg,
        "TOTP_SETUP_TOKEN_MISSING",
        HttpStatus.UNAUTHORIZED,
        "Thrown when the TOTP setup token is missing.",
        cause
    )

    companion object {

        /**
         * Converts a [TokenExtractionException] into an equivalent [TotpSetupTokenExtractionException].
         *
         * This method maps specific subclasses of [TokenExtractionException] to the corresponding
         * exception types in the context of TOTP setup token handling.
         *
         * @param ex The instance of [TokenExtractionException] to be converted.
         * @return An instance of [TotpSetupTokenExtractionException] that reflects the type and details of the input exception.
         */
        fun fromTokenExtractionException(ex: TokenExtractionException): TotpSetupTokenExtractionException {
            return when (ex) {
                is TokenExtractionException.Expired -> Expired(ex.message, ex.cause)
                is TokenExtractionException.Invalid -> Invalid(ex.message, ex.cause)
                is TokenExtractionException.Missing -> Missing(ex.message, ex.cause)
            }
        }
    }

}
