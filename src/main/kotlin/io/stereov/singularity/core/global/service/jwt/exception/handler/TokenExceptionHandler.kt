package io.stereov.singularity.core.global.service.jwt.exception.handler

import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.global.service.jwt.exception.TokenException
import io.stereov.singularity.core.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.core.global.service.jwt.exception.model.TokenExpiredException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * # Global exception handler for token-related exceptions.
 *
 * This class handles exceptions related to token operations and returns appropriate HTTP responses.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ControllerAdvice
class TokenExceptionHandler : BaseExceptionHandler<TokenException> {

    override fun getHttpStatus(ex: TokenException) = when (ex) {
        is TokenExpiredException -> HttpStatus.UNAUTHORIZED
        is InvalidTokenException -> HttpStatus.UNAUTHORIZED
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(TokenException::class)
    override fun handleException(
        ex: TokenException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
