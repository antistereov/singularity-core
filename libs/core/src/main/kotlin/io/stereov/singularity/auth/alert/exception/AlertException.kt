package io.stereov.singularity.auth.alert.exception

import io.stereov.singularity.email.core.exception.*
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class AlertException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception indicating that an alert could not be sent due to a failure in the mail sender.
     *
     * This exception is a specific type of [AlertException] and represents failures during
     * the process of sending an alert, such as when an underlying exception occurs in the
     * mail-sending functionality. It provides additional context about the error, including
     * an error message, unique error code, HTTP status, and a detailed description of the issue.
     *
     * @param msg The error message describing the failure to send the alert.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailSendFailure
     */
    class Send(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        EmailSendFailure.CODE,
        EmailSendFailure.STATUS,
        EmailSendFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception thrown during the creation of the alert email template.
     *
     * This exception extends the [AlertException] class. It is specifically used to indicate
     * failure scenarios while generating or processing alert email templates within the application.
     *
     *
     * @param msg The error message describing the issue.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailTemplateFailure
     */
    class Template(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        EmailTemplateFailure.CODE,
        EmailTemplateFailure.STATUS,
        EmailTemplateFailure.DESCRIPTION,
        cause
    )

    /**
     * Specific subclass of [AlertException] representing a failure during email authentication.
     *
     * This exception is intended to represent the scenario where the authentication process for sending an email fails.
     * It carries a predefined code, status, and description associated with email authentication errors.
     *
     * @param msg The message describing the cause of the authentication failure.
     * @param cause The underlying cause of the authentication failure, if any (can be null).
     * 
     * @see EmailAuthenticationFailure
     */
    class EmailAuthentication(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        EmailAuthenticationFailure.CODE,
        EmailAuthenticationFailure.STATUS,
        EmailAuthenticationFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception thrown when an email-related action cannot be performed because email functionality
     * has been disabled. This typically occurs in scenarios where sending emails is restricted or
     * explicitly turned off.
     *
     * @param msg The detailed message explaining the reason for the exception.
     * @param cause An optional underlying cause of this exception, if applicable.
     * 
     * @see EmailDisabledFailure
     */
    class EmailDisabled(msg: String, cause: Throwable? = null) : AlertException(
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
     * This exception is a specific type of [AlertException], used to enforce cooldown
     * restrictions on sending alerts. It provides additional context about the error,
     * including an error message, unique error code, HTTP status, and a detailed description
     * of the issue.
     *
     * @param msg The error message describing the cooldown restriction.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailCooldownActiveFailure
     */
    class CooldownActive(msg: String, cause: Throwable? = null) : AlertException(
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
     * It extends the [AlertException] class, inheriting its properties and behavior, and adds
     * additional context through its specific error code and description.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailCooldownCacheFailure
     */
    class CooldownCache(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        EmailCooldownCacheFailure.CODE,
        EmailCooldownCacheFailure.STATUS,
        EmailCooldownCacheFailure.DESCRIPTION,
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
