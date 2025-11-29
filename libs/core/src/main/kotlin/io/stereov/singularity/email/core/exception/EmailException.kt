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
     * @see EmailDisabledFailure
     */
    class Disabled(msg: String, cause: Throwable? = null) : EmailException(
        msg,
        EmailDisabledFailure.CODE,
        EmailDisabledFailure.STATUS,
        EmailDisabledFailure.DESCRIPTION,
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
     * @see EmailTemplateFailure
     */
    class Template(msg: String, cause: Throwable? = null): EmailException(
        msg,
        EmailTemplateFailure.CODE,
        EmailTemplateFailure.STATUS,
        EmailTemplateFailure.DESCRIPTION,
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
     * @see EmailAuthenticationFailure
     */
    class Authentication(msg: String, cause: Throwable? = null): EmailException(
        msg,
        EmailAuthenticationFailure.CODE,
        EmailAuthenticationFailure.STATUS,
        EmailAuthenticationFailure.DESCRIPTION,
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
     * @see EmailSendFailure
     */
    class Send(msg: String, cause: Throwable? = null): EmailException(
        msg,
        EmailSendFailure.CODE,
        EmailSendFailure.STATUS,
        EmailSendFailure.DESCRIPTION,
        cause
    ) {

    }
}