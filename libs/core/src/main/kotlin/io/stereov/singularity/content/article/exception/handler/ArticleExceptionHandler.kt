package io.stereov.singularity.content.article.exception.handler

import io.stereov.singularity.content.article.exception.ArticleException
import io.stereov.singularity.content.article.exception.model.InvalidArticleRequestException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
@ConditionalOnProperty(prefix = "singularity.content.articles", value = ["enable"], havingValue = "true", matchIfMissing = false)
class ArticleExceptionHandler : BaseExceptionHandler<ArticleException> {

    override fun getHttpStatus(ex: ArticleException) = when (ex) {
        is InvalidArticleRequestException -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(ArticleException::class)
    override fun handleException(
        ex: ArticleException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
