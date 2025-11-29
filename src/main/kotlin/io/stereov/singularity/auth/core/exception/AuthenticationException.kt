package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents authentication-related exceptions within the application.
 *
 * Serves as a base class for handling various authentication-specific scenarios, extending the
 * [SingularityException]. It provides attributes for an error message, unique error code,
 * HTTP status, a detailed description, and an optional cause.
 *
 * The following subclasses specialize the [AuthenticationException] for specific authentication
 * error conditions:
 *
 * - [AuthenticationRequired]
 * - [RoleRequired]
 * - [GroupMembershipRequired]
 * - [AlreadyAuthenticated]
 *
 * Each subclass represents a distinct use case associated with authentication errors.
 *
 * @param msg The exception message providing details about the error.
 * @param code A unique code representing the error type.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description of the error context.
 * @param cause The optional underlying cause of the exception.
 */
sealed class AuthenticationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating that authentication is required for the requested operation.
     *
     * This exception is typically thrown when an unauthenticated user attempts to perform an action
     * requiring authentication. It carries additional context including a unique error code, HTTP status,
     * a descriptive message, and an optional underlying cause.
     *
     * @param msg The exception message providing details about the specific error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `AUTHENTICATION_REQUIRED`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class AuthenticationRequired(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        "AUTHENTICATION_REQUIRED",
        HttpStatus.UNAUTHORIZED,
        "User is not authenticated.",
        cause
    )

    /**
     * Represents a specific type of [AuthenticationException] indicating that the user does not
     * have the required role to perform an action or access particular resources.
     *
     * This exception is typically thrown when the authorization process determines
     * that the user's role is inadequate to fulfill the requested operation.
     *
     * @param msg The error message describing the exception.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `ROLE_REQUIRED`
     * @property status [HttpStatus.FORBIDDEN]
     */
    class RoleRequired(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        "ROLE_REQUIRED",
        HttpStatus.FORBIDDEN,
        "User does not have required role.",
        cause
    )

    /**
     * Represents a specific type of [AuthenticationException] indicating that the user
     * is not a member of the required group.
     *
     * This exception is typically thrown when an authenticated user attempts to perform
     * an operation or access a resource that requires membership in a specific group, but
     * the user does not satisfy that requirement.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_MEMBERSHIP_REQUIRED`
     * @property status [HttpStatus.FORBIDDEN]
     * @property description "User is not a member of required group."
     */
    class GroupMembershipRequired(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        "GROUP_MEMBERSHIP_REQUIRED",
        HttpStatus.FORBIDDEN,
        "User is not a member of required group.",
        cause
    )

    /**
     * Represents a specific type of [AuthenticationException] indicating that the user is already authenticated.
     *
     * This exception is typically thrown when there is an attempt to authenticate an already authenticated user.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `ALREADY_AUTHENTICATED`
     * @property status [HttpStatus.NOT_MODIFIED]
     */
    class AlreadyAuthenticated(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        AlreadyAuthenticatedFailure.CODE,
        AlreadyAuthenticatedFailure.STATUS,
        AlreadyAuthenticatedFailure.DESCRIPTION,
        cause
    )
}
