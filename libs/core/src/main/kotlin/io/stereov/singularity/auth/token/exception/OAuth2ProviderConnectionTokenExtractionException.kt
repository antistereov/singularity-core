package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.token.model.OAuth2ProviderConnectionToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a hierarchy of exceptions specifically related to the extraction of OAuth2 provider connection tokens.
 *
 * This sealed class is a specialization of [SingularityException], providing detailed error codes,
 * HTTP statuses, and additional context for scenarios where extraction of OAuth2 provider connection tokens fails.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure during token extraction.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class OAuth2ProviderConnectionTokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the [OAuth2ProviderConnectionToken] cannot be decoded.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `OAUTH2_PROVIDER_CONNECTION_TOKEN_INVALID`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Invalid
     */ 
    class Invalid(msg: String, cause: Throwable? = null) : OAuth2ProviderConnectionTokenExtractionException(
        msg,
        "OAUTH2_PROVIDER_CONNECTION_TOKEN_INVALID",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the OAuth2 provider connection token cannot be decoded.",
        cause
    )

    /**
     * Indicates that the [OAuth2ProviderConnectionToken] is expired.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code The error code `OAUTH2_PROVIDER_CONNECTION_TOKEN_EXPIRED`
     * @property status The status [HttpStatus.UNAUTHORIZED]
     *
     * @see TokenExtractionException.Expired
     */
    class Expired(msg: String, cause: Throwable? = null) : OAuth2ProviderConnectionTokenExtractionException(
        msg,
        "OAUTH2_PROVIDER_CONNECTION_TOKEN_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the OAuth2 provider connection token is expired.",
        cause
    )

    /**
     * Thrown when the [OAuth2ProviderConnectionToken] is missing.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `OAUTH2_PROVIDER_CONNECTION_TOKEN_MISSING`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenExtractionException.Missing
     */
    class Missing(msg: String, cause: Throwable? = null) : OAuth2ProviderConnectionTokenExtractionException(
        msg,
        "OAUTH2_PROVIDER_CONNECTION_TOKEN_MISSING",
        HttpStatus.UNAUTHORIZED,
        "Thrown when the OAuth2 provider connection token is missing.",
        cause
    )

    companion object {

        /**
         * Converts a [TokenExtractionException] into an equivalent [OAuth2ProviderConnectionTokenExtractionException].
         *
         * This method maps specific subclasses of [TokenExtractionException] to the corresponding
         * exception types in the context of OAuth2 provider connection token handling.
         *
         * @param ex The instance of [TokenExtractionException] to be converted.
         * @return An instance of [OAuth2ProviderConnectionTokenExtractionException] that reflects the type and details of the input exception.
         */
        fun fromTokenExtractionException(ex: TokenExtractionException): OAuth2ProviderConnectionTokenExtractionException {
            return when (ex) {
                is TokenExtractionException.Expired -> Expired(ex.message, ex.cause)
                is TokenExtractionException.Invalid -> Invalid(ex.message, ex.cause)
                is TokenExtractionException.Missing -> Missing(ex.message, ex.cause)
            }
        }
    }

}
