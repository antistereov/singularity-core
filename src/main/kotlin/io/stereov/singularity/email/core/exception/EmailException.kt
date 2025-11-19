package io.stereov.singularity.email.core.exception

import io.stereov.singularity.global.exception.SingularityException

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
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Represents an exception indicating that email functionality is disabled.
     *
     * This exception is a specific subclass of [EmailException] with a predefined error code "EMAIL_DISABLED".
     * It is typically used to provide a clear indication when an email-related action cannot be performed
     * due to the email service being disabled in the application configuration.
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     */
    class Disabled(msg: String, cause: Throwable? = null) : EmailException(msg, CODE, cause) {
        companion object { const val CODE = "EMAIL_DISABLED" }
    }

    /**
     * Represents a specific type of [EmailException] that occurs when there is a failure
     * related to email template creation.
     *
     * This exception is typically used to signal issues encountered while generating or
     * processing email templates. It is identified with a predefined error code
     * "EMAIL_TEMPLATE_FAILURE".
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     */
    class Template(msg: String, cause: Throwable? = null): EmailException(msg, CODE, cause) {
        companion object { const val CODE = "EMAIL_TEMPLATE_FAILURE" }
    }

    /**
     * Represents a specific type of [EmailException] that occurs when there is a failure
     * related to email authentication.
     *
     * This exception is typically thrown to signal issues with email authentication,
     * such as incorrect credentials or problems with the email server configuration.
     *
     * It is identified by a predefined error code "EMAIL_AUTHENTICATION_FAILURE".
     *
     * @param msg The error message describing the exception.
     * @param cause The underlying cause of the exception, if any.
     */
    class Authentication(msg: String, cause: Throwable? = null): EmailException(msg, CODE, cause) {
        companion object { const val CODE = "EMAIL_AUTHENTICATION_FAILURE" }
    }

    /**
     * Represents an exception that occurs when an email cannot be sent.
     *
     * @constructor Creates an instance of the Send exception.
     * @param msg The error message describing the issue.
     * @param cause The underlying cause of the exception, if any.
     */
    class Send(msg: String, cause: Throwable? = null): EmailException(msg, CODE, cause) {
        companion object { const val CODE = "EMAIL_SEND_FAILURE" }
    }
}