package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class SendEmailAuthenticationException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "An error occurred during after the operation was successfully committed.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "INVALID_USER_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid user document.",
        cause
    )

    class CooldownCache(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "COOLDOWN_CACHE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Too many requests. Please try again later.",
        cause
    )

    class CooldownActive(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "COOLDOWN_ACTIVE",
        HttpStatus.TOO_MANY_REQUESTS,
        "Two factor authentication is currently active. Please wait until the cooldown expires.",
        cause
    )

    class Template(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "EMAIL_TEMPLATE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to render email template.",
        cause
    )

    class EmailAuthentication(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "EMAIL_AUTHENTICATION_FAILURE",
        HttpStatus.UNAUTHORIZED,
        "Thrown when an email authentication failure occurs.",
        cause
    )

    class EmailDisabled(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "EMAIL_DISABLED",
        HttpStatus.FORBIDDEN,
        "Thrown when an email is disabled.",
        cause
    )

    class Send(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        "EMAIL_SEND_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Thrown when an email authentication cannot be sent due to an exception that occurred in the mail sender.",
        cause
    )


}
