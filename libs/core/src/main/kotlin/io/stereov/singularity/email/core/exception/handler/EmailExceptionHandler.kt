package io.stereov.singularity.email.core.exception.handler

import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.email.core.exception.EmailException
import io.stereov.singularity.email.core.exception.model.EmailCooldownException
import io.stereov.singularity.email.core.exception.model.EmailDisabledException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class EmailExceptionHandler : BaseExceptionHandler<EmailException> {

    override fun getHttpStatus(ex: EmailException) = when (ex) {
        is EmailCooldownException -> HttpStatus.TOO_MANY_REQUESTS
        is EmailDisabledException -> HttpStatus.SERVICE_UNAVAILABLE
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(EmailException::class)
    override fun handleException(
        ex: EmailException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
