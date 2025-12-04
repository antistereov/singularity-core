package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.token.model.OAuth2ProviderConnectionToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Exception hierarchy related to failures during the creation of an [OAuth2ProviderConnectionToken].
 *
 * This sealed class serves as the base for specific exceptions that occur
 * when creating an [OAuth2ProviderConnectionToken], providing detailed error codes and HTTP
 * statuses to handle various scenarios effectively.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure within [OAuth2ProviderConnectionToken] creation.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the specific failure.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class OAuth2ProviderConnectionTokenCreationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception that occurs during the encoding process of an [OAuth2ProviderConnectionToken].
     *
     * Extends [OAuth2ProviderConnectionTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `OAUTH2_PROVIDER_CONNECTION_TOKEN_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Encoding
     */
    class Encoding(msg: String, cause: Throwable? = null) : OAuth2ProviderConnectionTokenCreationException(
        msg,
        "OAUTH2_PROVIDER_CONNECTION_TOKEN_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the encoding process of an OAuth2 provider connection token.",
        cause
    )

    /**
     * Represents an exception that occurs when there is a failure related to the secret required for creating an [OAuth2ProviderConnectionToken].
     *
     * Extends [OAuth2ProviderConnectionTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `OAUTH2_PROVIDER_CONNECTION_TOKEN_CACHE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Secret
     */
    class Secret(msg: String, cause: Throwable? = null) : OAuth2ProviderConnectionTokenCreationException(
        msg,
        "OAUTH2_PROVIDER_CONNECTION_TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to the secret required for creating an OAuth2 provider connection token.",
        cause
    )

    companion object {

        /**
         * Maps a [TokenCreationException] to the corresponding exception type
         * in the OAuth2 provider connection token creation exception hierarchy.
         *
         * @param ex The exception instance of type [TokenCreationException]
         *  that occurred during the token creation process.
         *  Can be an instance of [TokenCreationException.Encoding] or
         *  [TokenCreationException.Secret].
         */
        fun fromTokenCreationException(ex: TokenCreationException) = when (ex) {
            is TokenCreationException.Encoding -> Encoding(ex.message, ex.cause)
            is TokenCreationException.Secret -> Secret(ex.message, ex.cause)
        }
    }
}
