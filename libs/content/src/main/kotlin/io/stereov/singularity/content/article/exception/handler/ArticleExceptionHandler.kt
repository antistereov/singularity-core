package io.stereov.singularity.content.article.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.exception.model.InvalidArticleRequestException
import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.global.model.ErrorResponse
import io.stereov.singularity.content.article.exception.ArticleException
import io.stereov.singularity.content.article.exception.model.ArticleKeyExistsException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class ArticleExceptionHandler : BaseExceptionHandler<ArticleException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(ArticleException::class)
    override suspend fun handleException(
        ex: ArticleException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is ArticleKeyExistsException -> HttpStatus.CONFLICT
            is InvalidArticleRequestException -> HttpStatus.FORBIDDEN
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
