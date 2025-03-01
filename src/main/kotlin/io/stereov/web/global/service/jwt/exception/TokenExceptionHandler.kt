package io.stereov.web.global.service.jwt.exception

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class TokenExceptionHandler {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(TokenException::class)
    suspend fun handleTokenExceptions(
        ex: TokenException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is TokenExpiredException -> HttpStatus.UNAUTHORIZED
            is InvalidTokenException -> HttpStatus.UNAUTHORIZED
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
