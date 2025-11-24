package io.stereov.singularity.admin.core.exception.handler

import io.stereov.singularity.admin.core.exception.AdminException
import io.stereov.singularity.admin.core.exception.model.GuestCannotBeAdminException
import io.stereov.singularity.admin.core.exception.model.AtLeastOneAdminRequiredException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class AdminExceptionHandler : BaseExceptionHandler<AdminException> {

    override fun getHttpStatus(ex: AdminException) = when (ex) {
        is GuestCannotBeAdminException -> HttpStatus.BAD_REQUEST
        is AtLeastOneAdminRequiredException -> HttpStatus.CONFLICT
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(AdminException::class)
    override fun handleException(
        ex: AdminException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
