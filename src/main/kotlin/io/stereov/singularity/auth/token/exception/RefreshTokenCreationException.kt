package io.stereov.singularity.auth.token.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.token.model.RefreshToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Encapsulates exceptions that occur during the process of creating refresh tokens.
 *
 * This sealed class extends [SingularityException] and serves as the base class for
 * specific exceptions related to refresh token creation failures. It provides contextual
 * information such as an error message, error code, HTTP status, and an optional underlying cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure within refresh token creation.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the specific failure.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class RefreshTokenCreationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a failure that occurs during the creation of a [RefreshToken].
     *
     * Extends [RefreshTokenCreationException].
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `REFRESH_TOKEN_CREATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Failed(msg: String, cause: Throwable? = null) : RefreshTokenCreationException(
        msg,
        "REFRESH_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when a generic exception occurred during the creation of an access token.",
        cause
    )

    /**
     * Represents an exception that occurs during the session update process following the creation
     * of a new [RefreshToken].
     *
     * This exception is a specialization of [RefreshTokenCreationException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `SESSION_UPDATE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class SessionUpdate(msg: String, cause: Throwable? = null) : RefreshTokenCreationException(
        msg,
        "REFRESH_TOKEN_SESSION_UPDATE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an exception occurs when updating the user sessions after creating a new refresh token.",
        cause
    )

    /**
     * Represents an exception that occurs during the encoding process of a [RefreshToken].
     *
     * Extends [RefreshTokenCreationException].
     *
     * @param msg The error message describing the cause of the encoding failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `REFRESH_TOKEN_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Encoding
     */
    class Encoding(msg: String, cause: Throwable? = null) : RefreshTokenCreationException(
        msg,
        "REFRESH_TOKEN_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the encoding process of a refresh token.",
        cause
    )

    /**
     * Represents an exception that occurs when there is a failure related to the secret required
     * for creating a [RefreshToken].
     *
     * Extends [RefreshTokenCreationException].
     *
     * @param msg The error message describing the nature of the secret-related failure.
     * @param cause The underlying cause of this exception, if available.
     *
     * @property code `REFRESH_TOKEN_SECRET_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Secret(msg: String, cause: Throwable? = null) : RefreshTokenCreationException(
        msg,
        "REFRESH_TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to the secret required for creating a refresh token.",
        cause
    )

    companion object {
        /**
         * Maps a given [TokenCreationException] to a corresponding [RefreshTokenCreationException].
         *
         * This function is used to convert specific token creation exceptions to matching refresh token
         * creation exceptions, ensuring proper handling and consistency in error propagation.
         */
        fun fromTokenCreationException(ex: TokenCreationException): RefreshTokenCreationException {
            return when (ex) {
                is TokenCreationException.Encoding -> Encoding(ex.message, ex.cause)
                is TokenCreationException.Secret -> Secret(ex.message, ex.cause)
            }
        }
    }
}