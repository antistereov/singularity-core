package io.stereov.singularity.content.translate.exception.handler

import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.content.translate.exception.TranslationException
import io.stereov.singularity.content.translate.exception.model.TranslationForLangMissingException
import io.stereov.singularity.content.translate.exception.model.TranslationLanguageNotImplementedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class TranslationExceptionHandler : BaseExceptionHandler<TranslationException> {

    override fun getHttpStatus(ex: TranslationException) = when (ex) {
        is TranslationForLangMissingException -> HttpStatus.UNPROCESSABLE_ENTITY
        is TranslationLanguageNotImplementedException -> HttpStatus.UNPROCESSABLE_ENTITY
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(TranslationException::class)
    override fun handleException(ex: TranslationException, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)
}
