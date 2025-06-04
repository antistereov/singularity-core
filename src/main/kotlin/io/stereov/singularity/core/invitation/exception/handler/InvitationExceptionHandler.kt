package io.stereov.singularity.core.invitation.exception.handler

import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.invitation.exception.InvitationException
import io.stereov.singularity.core.invitation.exception.model.InvalidInvitationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class InvitationExceptionHandler : BaseExceptionHandler<InvitationException> {

    override fun getHttpStatus(ex: InvitationException) = when (ex) {
        is InvalidInvitationException -> HttpStatus.FORBIDDEN
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(InvitationException::class)
    override fun handleException(ex: InvitationException, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)
}
