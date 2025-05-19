package io.stereov.singularity.core.global.service.file.exception.handler

import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.global.service.file.exception.FileException
import io.stereov.singularity.core.global.service.file.exception.model.DeleteFailedException
import io.stereov.singularity.core.global.service.file.exception.model.FileSecurityException
import io.stereov.singularity.core.global.service.file.exception.model.NoSuchFileException
import io.stereov.singularity.core.global.service.file.exception.model.UnsupportedMediaTypeException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class FileExceptionHandler : BaseExceptionHandler<FileException> {

    override fun getHttpStatus(ex: FileException) = when (ex) {
        is DeleteFailedException -> HttpStatus.INTERNAL_SERVER_ERROR
        is NoSuchFileException -> HttpStatus.NOT_FOUND
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
