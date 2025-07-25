package io.stereov.singularity.content.common.content.exception.handler

import io.stereov.singularity.content.common.content.exception.ContentException
import io.stereov.singularity.content.common.content.exception.model.ContentKeyExistsException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class ContentExceptionHandler : BaseExceptionHandler<ContentException> {

    override fun getHttpStatus(ex: ContentException) = when (ex) {
        is ContentKeyExistsException -> HttpStatus.CONFLICT
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(ContentException::class)
    override fun handleException(
        ex: ContentException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
