package io.stereov.singularity.secrets.core.exception.handler

import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.secrets.core.exception.SecretsException
import io.stereov.singularity.secrets.core.exception.model.NoCurrentKeyException
import io.stereov.singularity.secrets.core.exception.model.SecretKeyNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class SecretsExceptionHandler : BaseExceptionHandler<SecretsException> {

    override fun getHttpStatus(ex: SecretsException) = when (ex) {
        is SecretKeyNotFoundException -> HttpStatus.INTERNAL_SERVER_ERROR
        is NoCurrentKeyException -> HttpStatus.INTERNAL_SERVER_ERROR
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(SecretsException::class)
    override fun handleException(
        ex: SecretsException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
