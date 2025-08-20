package io.stereov.singularity.auth.group.exception.handler

import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.auth.group.exception.GroupException
import io.stereov.singularity.auth.group.exception.model.GroupKeyExistsException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class GroupExceptionHandler : BaseExceptionHandler<GroupException> {

    override fun getHttpStatus(ex: GroupException) = when(ex) {
        is GroupKeyExistsException -> HttpStatus.CONFLICT
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(GroupException::class)
    override fun handleException(ex: GroupException, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)
}
