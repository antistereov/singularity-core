package io.stereov.web.global.service.mail.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.model.ErrorResponse
import io.stereov.web.global.service.mail.exception.MailException
import io.stereov.web.global.service.mail.exception.model.MailCooldownException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * # Global exception handler for mail-related exceptions.
 *
 * This class handles exceptions related to mail operations and returns appropriate HTTP responses.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ControllerAdvice
class MailExceptionHandler {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(MailException::class)
    suspend fun handleRateLimitExceptions(
        ex: MailException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is MailCooldownException -> HttpStatus.TOO_MANY_REQUESTS
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
