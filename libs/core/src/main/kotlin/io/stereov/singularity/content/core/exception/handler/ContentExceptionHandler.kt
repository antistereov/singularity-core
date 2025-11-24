package io.stereov.singularity.content.core.exception.handler

import io.stereov.singularity.content.core.exception.ContentException
import io.stereov.singularity.content.core.exception.model.ContentKeyExistsException
import io.stereov.singularity.content.core.exception.model.ContentTypeNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class ContentExceptionHandler : BaseExceptionHandler<ContentException> {

    override fun getHttpStatus(ex: ContentException) = when (ex) {
        is ContentKeyExistsException -> HttpStatus.CONFLICT
        is ContentTypeNotFoundException -> HttpStatus.NOT_FOUND
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(ContentException::class)
    override fun handleException(
        ex: ContentException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
