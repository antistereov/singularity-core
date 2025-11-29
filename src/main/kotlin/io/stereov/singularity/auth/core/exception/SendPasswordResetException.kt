package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.email.core.exception.*
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Base class representing exceptions related to sending password reset alerts.
 *
 * The `SendPasswordResetException` class serves as a base type for various specific exceptions
 * encountered during the process of sending password reset alerts or managing related operations.
 * Each subclass provides more granular details about specific error scenarios, along with
 * associated HTTP status codes, error codes, and descriptions.
 *
 * @param msg The error message describing the problem.
 * @param code A unique error code representing the type of failure.
 * @param status The associated HTTP status code for the failure.
 * @param description A detailed description of the failure.
 * @param cause The underlying cause of the failure, if available.
 */
sealed class SendPasswordResetException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception indicating that an alert could not be sent due to a failure in the mail sender.
     *
     * This exception is a specific type of [SendPasswordResetException] and represents failures during
     * the process of sending an alert, such as when an underlying exception occurs in the
     * mail-sending functionality. It provides additional context about the error, including
     * an error message, unique error code, HTTP status, and a detailed description of the issue.
     *
     * @param msg The error message describing the failure to send the alert.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailSendFailure
     */
    class Send(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        EmailSendFailure.CODE,
        EmailSendFailure.STATUS,
        EmailSendFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception indicating a failure during the email-based authentication process.
     *
     * This exception is thrown when there is an issue specific to authentication using
     * email, such as invalid email credentials or email verification failure.
     * It extends [SendPasswordResetException] to provide additional context,
     * including an error message, a specific error code, the HTTP status,
     * a detailed description of the error, and an optional underlying cause.
     *
     * @param msg The error message describing the exception.
     * @param cause The optional underlying cause of the exception.
     *
     * @see EmailAuthenticationFailure
     */
    class EmailAuthentication(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        EmailAuthenticationFailure.CODE,
        EmailAuthenticationFailure.STATUS,
        EmailAuthenticationFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception indicating that email functionality has been disabled.
     *
     * This exception is thrown when an operation related to password reset or email communication
     * is initiated, but email support has been explicitly disabled in the system.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see EmailDisabledFailure
     */
    class EmailDisabled(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        EmailDisabledFailure.CODE,
        EmailDisabledFailure.STATUS,
        EmailDisabledFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception indicating that an alert message was requested but cannot be sent
     * because the cooldown period is still active.
     *
     * This exception is a specific type of [SendPasswordResetException], used to enforce cooldown
     * restrictions on sending alerts. It provides additional context about the error,
     * including an error message, unique error code, HTTP status, and a detailed description
     * of the issue.
     *
     * @param msg The error message describing the cooldown restriction.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailCooldownActiveFailure
     */
    class CooldownActive(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        EmailCooldownActiveFailure.CODE,
        EmailCooldownActiveFailure.STATUS,
        EmailCooldownActiveFailure.DESCRIPTION,
        cause,
    )

    /**
     * Represents an exception specifically used for handling errors related to cooldown operations.
     *
     * This exception is typically thrown when there is an issue while setting or retrieving cooldowns.
     * It extends the [SendPasswordResetException] class, inheriting its properties and behavior, and adds
     * additional context through its specific error code and description.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailCooldownCacheFailure
     */
    class CooldownCache(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        EmailCooldownCacheFailure.CODE,
        EmailCooldownCacheFailure.STATUS,
        EmailCooldownCacheFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception thrown during the creation of the alert email template.
     *
     * This exception extends the [SendPasswordResetException] class. It is specifically used to indicate
     * failure scenarios while generating or processing alert email templates within the application.
     *
     *
     * @param msg The error message describing the issue.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ALERT_TEMPLATE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Template(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        EmailTemplateFailure.CODE,
        EmailTemplateFailure.STATUS,
        EmailTemplateFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception indicating a failure during an encrypted database operation.
     *
     * This exception is thrown when a database operation encounters an error that prevents it
     * from being completed successfully. Issues such as decryption failures, corrupted data, or
     * other internal database-related problems can trigger this exception.
     *
     * @param msg A message providing context about the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception indicating a failure during token generation.
     *
     * This exception is typically thrown when an error occurs while generating a token, such as
     * issues with encryption, invalid data, or service failures. It provides additional context
     * by including a unique error code, associated HTTP status, and a detailed description of the issue.
     *
     * @param msg The exception message providing details about the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `PASSWORD_RESET_TOKEN_CREATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Token(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "PASSWORD_RESET_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an error occurs during token generation.",
        cause
    )

    companion object {

        fun from(ex: EmailException) = when (ex) {
            is EmailException.Send -> Send(ex.message, ex.cause)
            is EmailException.Disabled -> EmailDisabled(ex.message, ex.cause)
            is EmailException.Template -> Template(ex.message, ex.cause)
            is EmailException.Authentication -> EmailAuthentication(ex.message, ex.cause)
        }
    }
}
