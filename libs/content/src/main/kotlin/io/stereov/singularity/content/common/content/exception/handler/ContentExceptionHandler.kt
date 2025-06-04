package io.stereov.singularity.content.common.content.exception.handler

import io.stereov.singularity.content.common.content.exception.ContentException
import io.stereov.singularity.content.common.content.exception.model.ContentKeyExistsException
import io.stereov.singularity.content.common.content.exception.model.InvalidInvitationException
import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class ContentExceptionHandler : BaseExceptionHandler<ContentException> {

    override fun getHttpStatus(ex: ContentException) = when (ex) {
        is ContentKeyExistsException -> HttpStatus.CONFLICT
        is InvalidInvitationException -> HttpStatus.FORBIDDEN
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(ContentException::class)
    override fun handleException(
        ex: ContentException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
