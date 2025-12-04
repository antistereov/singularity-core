package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.token.model.TwoFactorAuthenticationToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Exception hierarchy related to failures during the creation of an [TwoFactorAuthenticationToken].
 *
 * This sealed class serves as the base for specific exceptions that occur
 * when creating an [TwoFactorAuthenticationToken], providing detailed error codes and HTTP
 * statuses to handle various scenarios effectively.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure within [TwoFactorAuthenticationToken] creation.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the specific failure.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class TwoFactorAuthenticationTokenCreationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception that occurs during the encoding process of an [TwoFactorAuthenticationToken].
     *
     * Extends [TwoFactorAuthenticationTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `TWO_FACTOR_AUTHENTICATION_TOKEN_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Encoding
     */
    class Encoding(msg: String, cause: Throwable? = null) : TwoFactorAuthenticationTokenCreationException(
        msg,
        "TWO_FACTOR_AUTHENTICATION_TOKEN_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the encoding process of an two-factor authentication token.",
        cause
    )

    /**
     * Represents an exception that occurs when there is a failure related to the secret required for creating an [TwoFactorAuthenticationToken].
     *
     * Extends [TwoFactorAuthenticationTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `TWO_FACTOR_AUTHENTICATION_TOKEN_CACHE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Secret
     */
    class Secret(msg: String, cause: Throwable? = null) : TwoFactorAuthenticationTokenCreationException(
        msg,
        "TWO_FACTOR_AUTHENTICATION_TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to the secret required for creating an  two-factor authentication token.",
        cause
    )

    companion object {
        /**
         * Maps a [TokenCreationException] to a specific subclass of [TwoFactorAuthenticationTokenCreationException].
         * Converts the provided exception into a more specific exception type based on its kind.
         *
         * @param ex The [TokenCreationException] instance to be converted. It can be of type
         * [TokenCreationException.Encoding] or [TokenCreationException.Secret].
         * If the type is [TokenCreationException.Encoding], it will be mapped to [TwoFactorAuthenticationTokenCreationException.Encoding].
         * If the type is [TokenCreationException.Secret], it will be mapped to [TwoFactorAuthenticationTokenCreationException.Secret].
         */
        fun fromTokenCreationException(ex: TokenCreationException) = when (ex) {
            is TokenCreationException.Encoding -> Encoding(ex.message, ex.cause)
            is TokenCreationException.Secret -> Secret(ex.message, ex.cause)
        }
    }
}
