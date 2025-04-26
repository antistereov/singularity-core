package io.stereov.singularity.core.global.service.file.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.global.model.ErrorResponse
import io.stereov.singularity.core.global.service.file.exception.FileException
import io.stereov.singularity.core.global.service.file.exception.model.DeleteFailedException
import io.stereov.singularity.core.global.service.file.exception.model.FileSecurityException
import io.stereov.singularity.core.global.service.file.exception.model.NoSuchFileException
import io.stereov.singularity.core.global.service.file.exception.model.UnsupportedMediaTypeException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class FileExceptionHandler : BaseExceptionHandler<FileException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(FileException::class)
    override suspend fun handleException(
        ex: FileException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is DeleteFailedException -> HttpStatus.INTERNAL_SERVER_ERROR
            is NoSuchFileException -> HttpStatus.NOT_FOUND
            is FileSecurityException -> HttpStatus.FORBIDDEN
            is UnsupportedMediaTypeException -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val errorResponse = ErrorResponse(
            status = status.value(),
            error = ex.javaClass.simpleName,
            message = ex.message,
            path = exchange.request.uri.path
        )

        return ResponseEntity(errorResponse, status)
    }
}
