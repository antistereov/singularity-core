package io.stereov.singularity.file.core.exception.handler

import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.model.DeleteFailedException
import io.stereov.singularity.file.core.exception.model.FileNotFoundException
import io.stereov.singularity.file.core.exception.model.FileSecurityException
import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class FileExceptionHandler : BaseExceptionHandler<FileException> {

    override fun getHttpStatus(ex: FileException) = when (ex) {
        is DeleteFailedException -> HttpStatus.INTERNAL_SERVER_ERROR
        is FileNotFoundException -> HttpStatus.NOT_FOUND
        is FileSecurityException -> HttpStatus.FORBIDDEN
        is UnsupportedMediaTypeException -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(FileException::class)
    override fun handleException(
        ex: FileException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
