package io.stereov.singularity.auth.jwt.exception.handler

import io.stereov.singularity.auth.jwt.exception.model.TokenException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class TokenExceptionHandler : BaseExceptionHandler<TokenException> {

    override fun getHttpStatus(ex: TokenException) = when (ex) {
        is TokenException.Expired -> HttpStatus.UNAUTHORIZED
        is TokenException.Invalid -> HttpStatus.UNAUTHORIZED
        is TokenException.Missing -> HttpStatus.UNAUTHORIZED
    }

    @ExceptionHandler(TokenException::class)
    override fun handleException(
        ex: TokenException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}