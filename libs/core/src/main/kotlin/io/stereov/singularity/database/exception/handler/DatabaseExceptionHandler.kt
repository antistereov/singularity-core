package io.stereov.singularity.database.exception.handler

import io.stereov.singularity.database.exception.DatabaseException
import io.stereov.singularity.database.exception.model.UnexpectedContentTypeException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class DatabaseExceptionHandler : BaseExceptionHandler<DatabaseException> {

    override fun getHttpStatus(ex: DatabaseException) = when (ex) {
        is UnexpectedContentTypeException -> HttpStatus.INTERNAL_SERVER_ERROR
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(DatabaseException::class)
    override fun handleException(ex: DatabaseException, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)
}
