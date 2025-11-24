package io.stereov.singularity.content.tag.exception.handler

import io.stereov.singularity.content.tag.exception.TagException
import io.stereov.singularity.content.tag.exception.model.InvalidUpdateTagRequest
import io.stereov.singularity.content.tag.exception.model.TagKeyExistsException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class TagExceptionHandler : BaseExceptionHandler<TagException> {

    override fun getHttpStatus(ex: TagException) = when (ex) {
        is InvalidUpdateTagRequest -> HttpStatus.BAD_REQUEST
        is TagKeyExistsException -> HttpStatus.CONFLICT
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(TagException::class)
    override fun handleException(
        ex: TagException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
