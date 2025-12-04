package io.stereov.singularity.content.invitation.exception

import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Exception hierarchy related to failures during the creation of an invitation token.
 *
 * This sealed class serves as the base for specific exceptions that occur
 * when creating an invitation token, providing detailed error codes and HTTP
 * statuses to handle various scenarios effectively.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of failure within invitation token creation.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the specific failure.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class InvitationTokenCreationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception that occurs during the encoding process of an invitation token.
     *
     * Extends [InvitationTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `INVITATION_TOKEN_ENCODING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Encoding
     */
    class Encoding(msg: String, cause: Throwable? = null) : InvitationTokenCreationException(
        msg,
        "INVITATION_TOKEN_ENCODING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the encoding process of an invitation token.",
        cause
    )

    /**
     * Represents an exception that occurs when there is a failure related to the secret required for creating an invitation token.
     *
     * Extends [InvitationTokenCreationException].
     *
     * @param msg The error message describing the cause of the caching failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `INVITATION_TOKEN_CACHE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see TokenCreationException.Secret
     */
    class Secret(msg: String, cause: Throwable? = null) : InvitationTokenCreationException(
        msg,
        "INVITATION_TOKEN_SECRET_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when there is a failure related to the secret required for creating an invitation token.",
        cause
    )

    /**
     * Exception thrown to indicate that the principal document associated with an invitation token is invalid.
     *
     * This exception extends `InvitationTokenCreationException` and provides detailed context about
     * failures related to invalid principal documents during the invitation token creation process.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `INVALID_INVITATION_DOCUMENT`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class InvalidInvitation(msg: String, cause: Throwable? = null) : InvitationTokenCreationException(
        msg,
        "INVALID_INVITATION_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Indicates that the principal document associated with the invitation token is invalid.",
        cause
    )

    companion object {
        fun from(ex: TokenCreationException) = when (ex) {
            is TokenCreationException.Encoding -> Encoding(ex.message, ex.cause)
            is TokenCreationException.Secret -> Secret(ex.message, ex.cause)
        }
    }
}
