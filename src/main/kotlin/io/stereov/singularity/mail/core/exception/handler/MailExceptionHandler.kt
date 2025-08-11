package io.stereov.singularity.mail.core.exception.handler

import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.mail.core.exception.MailException
import io.stereov.singularity.mail.core.exception.model.MailCooldownException
import io.stereov.singularity.mail.core.exception.model.MailDisabledException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * # Global exception handler for mail-related exceptions.
 *
 * This class handles exceptions related to mail operations and returns appropriate HTTP responses.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ControllerAdvice
class MailExceptionHandler : BaseExceptionHandler<MailException> {

    override fun getHttpStatus(ex: MailException) = when (ex) {
        is MailCooldownException -> HttpStatus.TOO_MANY_REQUESTS
        is MailDisabledException -> HttpStatus.SERVICE_UNAVAILABLE
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(MailException::class)
    override fun handleException(
        ex: MailException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
