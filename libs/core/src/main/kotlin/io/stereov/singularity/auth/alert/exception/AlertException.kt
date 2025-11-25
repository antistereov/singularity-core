package io.stereov.singularity.auth.alert.exception

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
     * @property code `ALERT_SEND_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Send(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        "ALERT_SEND_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an alert cannot be sent due to an exception that occurred in the mail sender.",
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
     * @property code `ALERT_COOLDOWN_ACTIVE`
     * @property status [HttpStatus.TOO_MANY_REQUESTS]
     */
    class CooldownActive(msg: String, cause: Throwable? = null) : AlertException(
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
     * It extends the [AlertException] class, inheriting its properties and behavior, and adds
     * additional context through its specific error code and description.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if available.
     */
    class CooldownCache(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        "ALERT_COOLDOWN_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an excpetion occurs when setting or getting cooldown.",
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
     * @property code `ALERT_TEMPLATE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Template(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        "ALERT_TEMPLATE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an exception occurred when creating the alert email template.",
        cause
    )

    class EmailMissing(msg: String, cause: Throwable? = null) : AlertException(
        msg,
        "USER_HAS_NO_EMAIL",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when a stored user document does not contain an email address.",
        cause
    )
}