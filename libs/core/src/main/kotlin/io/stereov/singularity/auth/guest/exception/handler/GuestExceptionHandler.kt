package io.stereov.singularity.auth.guest.exception.handler

import io.stereov.singularity.auth.guest.exception.GuestException
import io.stereov.singularity.auth.guest.exception.model.AccountIsAlreadyUserException
import io.stereov.singularity.auth.guest.exception.model.GuestCannotPerformThisActionException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class GuestExceptionHandler : BaseExceptionHandler<GuestException> {

    override fun getHttpStatus(ex: GuestException) = when (ex) {
        is GuestCannotPerformThisActionException -> HttpStatus.BAD_REQUEST
        is AccountIsAlreadyUserException -> HttpStatus.NOT_MODIFIED
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(GuestException::class)
    override fun handleException(
        ex: GuestException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
