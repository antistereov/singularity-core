package io.stereov.singularity.file.core.exception.handler

import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.model.*
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class FileExceptionHandler : BaseExceptionHandler<FileException> {

    override fun getHttpStatus(ex: FileException) = when (ex) {
        is DeletingMetadataIsForbiddenException -> HttpStatus.BAD_REQUEST
        is FileKeyAlreadyTakenException -> HttpStatus.CONFLICT
        is FileNotFoundException -> HttpStatus.NOT_FOUND
        is FileSecurityException -> HttpStatus.FORBIDDEN
        is FileTooLargeException -> HttpStatus.REQUEST_ENTITY_TOO_LARGE
        is FileUploadException -> HttpStatus.BAD_REQUEST
        is UnsupportedMediaTypeException -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(FileException::class)
    override fun handleException(
        ex: FileException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
