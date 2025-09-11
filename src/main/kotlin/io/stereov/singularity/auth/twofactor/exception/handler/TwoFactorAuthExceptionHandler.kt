package io.stereov.singularity.auth.twofactor.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.twofactor.exception.TwoFactorAuthException
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorCodeException
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorRequestException
import io.stereov.singularity.auth.twofactor.exception.model.TwoFactorCodeExpiredException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class TwoFactorAuthExceptionHandler : BaseExceptionHandler<TwoFactorAuthException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override fun getHttpStatus(ex: TwoFactorAuthException) = when (ex) {
        is InvalidTwoFactorCodeException -> HttpStatus.UNAUTHORIZED
        is InvalidTwoFactorRequestException -> HttpStatus.BAD_REQUEST
        is TwoFactorCodeExpiredException -> HttpStatus.UNAUTHORIZED
        else -> HttpStatus.UNAUTHORIZED
    }

    @ExceptionHandler(TwoFactorAuthException::class)
    override fun handleException(
        ex: TwoFactorAuthException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
