package io.stereov.singularity.email.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a hierarchy of exceptions specific to email-related operations.
 *
 * This sealed class serves as the base class for various email-related exceptions in the application.
 * Each subclass corresponds to a specific type of email error and is associated with a predefined error code.
 *
 * @param msg The error message describing the exception.
 * @param code The error code identifying the type of the exception.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class EmailException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Thrown when email functionality is disabled in the application.
     *
     * This exception is a specific subclass of [EmailException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `EMAIL_DISABLED`
     * @property status [HttpStatus.SERVICE_UNAVAILABLE]
     */
    class Disabled(msg: String, cause: Throwable? = null) : EmailException(
        msg,
        "EMAIL_DISABLED",
        HttpStatus.SERVICE_UNAVAILABLE,
        "Thrown when email functionality is disabled in the application.",
        cause
    )

    /**
     * Thrown when there is a failure related to email template creation.
     *
     * This exception is a specific subclass of [EmailException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `EMAIL_TEMPLATE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Template(msg: String, cause: Throwable? = null): EmailException(
        msg,
        "EMAIL_TEMPLATE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when there is a failure related to email template creation.",
        cause
    )

    /**
     * Thrown when there is a failure related to email authentication.
     *
     * This exception is a subclass of [EmailException].
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `EMAIL_AUTHENTICATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Authentication(msg: String, cause: Throwable? = null): EmailException(
        msg,
        "EMAIL_AUTHENTICATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when there is a failure related to email authentication.",
        cause
    )

    /**
     * Represents an exception that occurs when an email cannot be sent.
     *
     * This exception is a subclass of [EmailException].
     *
     * @param msg The error message describing the issue.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `EMAIL_SEND_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Send(msg: String, cause: Throwable? = null): EmailException(
        msg,
        "EMAIL_SEND_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs when an email cannot be sent.",
        cause
    )
}