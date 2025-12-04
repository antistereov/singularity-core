package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.email.core.exception.*
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Base exception class for errors encountered during verification email operations.
 *
 * The `SendVerificationEmailException` is a sealed class that defines a hierarchy of exceptions
 * related to the process of sending or handling verification emails. Subclasses represent
 * specific error scenarios, each providing detailed information about the error with a specific
 * error code, HTTP status, and optional cause.
 *
 * @param msg The error message describing the context of the exception.
 * @param code The unique error code associated with the exception.
 * @param status The HTTP status code representing the error.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class SendVerificationEmailException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that a user's email has already been verified.
     *
     * This exception is thrown when an operation to resend a verification email
     * is attempted, but the email for the specific user has already been successfully verified.
     *
     * @param msg The exception message describing the error context.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `EMAIL_ALREADY_VERIFIED`
     * @property status [HttpStatus.NOT_MODIFIED]
     */
    class AlreadyVerified(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
        msg,
        "EMAIL_ALREADY_VERIFIED",
        HttpStatus.NOT_MODIFIED,
        "User's email is already verified.",
        cause
    )

    /**
     * Exception indicating that an alert could not be sent due to a failure in the mail sender.
     *
     * This exception is a specific type of [SendVerificationEmailException] and represents failures during
     * the process of sending an alert, such as when an underlying exception occurs in the
     * mail-sending functionality. It provides additional context about the error, including
     * an error message, unique error code, HTTP status, and a detailed description of the issue.
     *
     * @param msg The error message describing the failure to send the alert.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailSendFailure
     */
    class Send(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
        msg,
        EmailSendFailure.CODE,
        EmailSendFailure.STATUS,
        EmailSendFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents a specific type of [SendVerificationEmailException] thrown during the email authentication process.
     *
     * This exception is designed to handle scenarios where an email authentication failure occurs.
     * It extends [SendVerificationEmailException], providing additional context such as a unique error code,
     * HTTP status, detailed error description, and an optional cause for the exception.
     *
     * @param msg The error message describing the context of the email authentication failure.
     * @param cause The optional underlying cause of the exception that triggered the failure.
     *
     * @see EmailAuthenticationFailure
     */
    class EmailAuthentication(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
        msg,
        EmailAuthenticationFailure.CODE,
        EmailAuthenticationFailure.STATUS,
        EmailAuthenticationFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents a specific type of [SendVerificationEmailException] indicating that email functionality is disabled.
     *
     * This exception is thrown when an attempt to send a verification email is made, but the email functionality
     * has been explicitly disabled, either due to system configurations or policies. It provides additional
     * context by including a unique error code, an associated HTTP status, and a detailed description of the issue.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see EmailDisabledFailure
     */
    class EmailDisabled(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
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
     * This exception is a specific type of [SendVerificationEmailException], used to enforce cooldown
     * restrictions on sending alerts. It provides additional context about the error,
     * including an error message, unique error code, HTTP status, and a detailed description
     * of the issue.
     *
     * @param msg The error message describing the cooldown restriction.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailCooldownActiveFailure
     */
    class CooldownActive(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
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
     * It extends the [SendVerificationEmailException] class, inheriting its properties and behavior, and adds
     * additional context through its specific error code and description.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see EmailCooldownCacheFailure
     */
    class CooldownCache(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
        msg,
        EmailCooldownCacheFailure.CODE,
        EmailCooldownCacheFailure.STATUS,
        EmailCooldownCacheFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception thrown during the creation of the alert email template.
     *
     * This exception extends the [SendVerificationEmailException] class. It is specifically used to indicate
     * failure scenarios while generating or processing alert email templates within the application.
     *
     *
     * @param msg The error message describing the issue.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `ALERT_TEMPLATE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Template(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
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
     * from being completed successfully. This may occur due to issues such as decryption failures,
     * corrupted data, or other internal database-related problems.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception related to token generation failures.
     *
     * This exception is thrown when an error occurs during the token generation process,
     * typically used in contexts where generating a token is essential for authentication
     * or other related operations.
     *
     * @param msg The exception message providing details about the failure.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `TOKEN_GENERATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see io.stereov.singularity.auth.jwt.exception.TokenCreationException
     */
    class EmailVerificationTokenCreation(msg: String, cause: Throwable? = null) : SendVerificationEmailException(
        msg,
        "EMAIL_VERIFICATION_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an error occurs during token creation.",
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
