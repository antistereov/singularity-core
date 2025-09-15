package io.stereov.singularity.auth.twofactor.exception.handler

import io.stereov.singularity.auth.twofactor.exception.TwoFactorAuthException
import io.stereov.singularity.auth.twofactor.exception.model.*
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class TwoFactorAuthExceptionHandler : BaseExceptionHandler<TwoFactorAuthException> {

    override fun getHttpStatus(ex: TwoFactorAuthException) = when (ex) {
        is CannotDisableOnly2FAMethodException -> HttpStatus.BAD_REQUEST
        is InvalidTwoFactorCodeException -> HttpStatus.UNAUTHORIZED
        is InvalidTwoFactorRequestException -> HttpStatus.BAD_REQUEST
        is TwoFactorMethodSetupException -> HttpStatus.BAD_REQUEST
        is TwoFactorCodeExpiredException -> HttpStatus.FORBIDDEN
        is TwoFactorMethodDisabledException -> HttpStatus.FORBIDDEN
        else -> HttpStatus.UNAUTHORIZED
    }

    @ExceptionHandler(TwoFactorAuthException::class)
    override fun handleException(
        ex: TwoFactorAuthException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
