package io.stereov.singularity.file.exception.handler

import io.stereov.singularity.file.exception.model.DeleteFailedException
import io.stereov.singularity.file.exception.model.FileSecurityException
import io.stereov.singularity.file.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.file.exception.model.NoSuchFileException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class FileExceptionHandler : BaseExceptionHandler<io.stereov.singularity.file.exception.FileException> {

    override fun getHttpStatus(ex: io.stereov.singularity.file.exception.FileException) = when (ex) {
        is DeleteFailedException -> HttpStatus.INTERNAL_SERVER_ERROR
        is NoSuchFileException -> HttpStatus.NOT_FOUND
        is FileSecurityException -> HttpStatus.FORBIDDEN
        is UnsupportedMediaTypeException -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(io.stereov.singularity.file.exception.FileException::class)
    override fun handleException(
        ex: io.stereov.singularity.file.exception.FileException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
