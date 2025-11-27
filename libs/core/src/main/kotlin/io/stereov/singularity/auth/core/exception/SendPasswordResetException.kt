package io.stereov.singularity.auth.core.exception

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
     * @property code `ALERT_SEND_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Send(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "ALERT_SEND_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an alert cannot be sent due to an exception that occurred in the mail sender.",
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
     * @property code `EMAIL_AUTHENTICATION_FAILURE`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class EmailAuthentication(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "EMAIL_AUTHENTICATION_FAILURE",
        HttpStatus.UNAUTHORIZED,
        "Thrown when an email authentication failure occurs.",
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
     * @property code `EMAIL_DISABLED`
     * @property status [HttpStatus.SERVICE_UNAVAILABLE]
     */
    class EmailDisabled(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "EMAIL_DISABLED",
        HttpStatus.SERVICE_UNAVAILABLE,
        "Thrown when an email is disabled.",
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
     * @property code `ALERT_COOLDOWN_ACTIVE`
     * @property status [HttpStatus.TOO_MANY_REQUESTS]
     */
    class CooldownActive(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "ALERT_COOLDOWN_ACTIVE",
        HttpStatus.TOO_MANY_REQUESTS,
        "Thrown when an alert message was requested but the cooldown is active.",
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
     */
    class CooldownCache(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "ALERT_COOLDOWN_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an excpetion occurs when setting or getting cooldown.",
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
        "ALERT_TEMPLATE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an exception occurred when creating the alert email template.",
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
     * @property code `DATABASE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
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
     * @property code `TOKEN_GENERATION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Token(msg: String, cause: Throwable? = null) : SendPasswordResetException(
        msg,
        "TOKEN_GENERATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an error occurs during token generation.",
        cause
    )
}
