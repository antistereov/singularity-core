package io.stereov.singularity.global.exception.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.global.model.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class GlobalExceptionHandler {


    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(SingularityException::class)
    fun handleExceptionInternal(ex: SingularityException, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = ex.status

        val errorResponse = ErrorResponse(
            status = status.value(),
            error = ex.javaClass.simpleName,
            message = ex.message,
            path = exchange.request.uri.path,
            code = ex.code
        )

        return ResponseEntity(errorResponse, status)
    }
}
