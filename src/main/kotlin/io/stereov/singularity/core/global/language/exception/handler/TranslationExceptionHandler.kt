package io.stereov.singularity.core.global.language.exception.handler

import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.global.language.exception.TranslationException
import io.stereov.singularity.core.global.language.exception.model.TranslationForLangMissingException
import io.stereov.singularity.core.global.language.exception.model.TranslationLanguageNotImplementedException
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
