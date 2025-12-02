package io.stereov.singularity.global.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.model.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class SingularityExceptionHandler {


    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(SingularityException::class)
    fun handleException(ex: SingularityException, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = ex.status

        val errorResponse = ErrorResponse(
            exception =  ex,
            exchange = exchange
        )

        return ResponseEntity(errorResponse, status)
    }
}