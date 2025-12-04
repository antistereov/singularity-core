package io.stereov.singularity.content.invitation.exception

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.content.invitation.model.InvitationToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a hierarchy of exceptions specifically related to the extraction of invitation tokens.
 *
 * This sealed class is a specialization of [SingularityException], providing detailed error codes,
 * HTTP statuses, and additional context for scenarios where extraction of invitation tokens fails.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure during token extraction.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class InvitationTokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the [InvitationToken] cannot be decoded.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `INVITATION_TOKEN_INVALID`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Invalid
     */ 
    class Invalid(msg: String, cause: Throwable? = null) : InvitationTokenExtractionException(
        msg,
        "INVITATION_TOKEN_INVALID",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the invitation token cannot be decoded.",
        cause
    )

    /**
     * Indicates that the [InvitationToken] is expired.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `INVITATION_TOKEN_EXPIRED`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Expired
     */
    class Expired(msg: String, cause: Throwable? = null) : InvitationTokenExtractionException(
        msg,
        "INVITATION_TOKEN_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the invitation token is expired.",
        cause
    )

    /**
     * Thrown when the [InvitationToken] is missing.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `INVITATION_TOKEN_MISSING`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenExtractionException.Missing
     */
    class Missing(msg: String, cause: Throwable? = null) : InvitationTokenExtractionException(
        msg,
        "INVITATION_TOKEN_MISSING",
        HttpStatus.UNAUTHORIZED,
        "Thrown when the invitation token is missing.",
        cause
    )

    companion object {

        /**
         * Converts a [TokenExtractionException] into an equivalent [InvitationTokenExtractionException].
         *
         * This method maps specific subclasses of [TokenExtractionException] to the corresponding
         * exception types in the context of invitation token handling.
         *
         * @param ex The instance of [TokenExtractionException] to be converted.
         * @return An instance of [InvitationTokenExtractionException] that reflects the type and details of the input exception.
         */
        fun from(ex: TokenExtractionException): InvitationTokenExtractionException {
            return when (ex) {
                is TokenExtractionException.Expired -> Expired(ex.message, ex.cause)
                is TokenExtractionException.Invalid -> Invalid(ex.message, ex.cause)
                is TokenExtractionException.Missing -> Missing(ex.message, ex.cause)
            }
        }
    }

}
