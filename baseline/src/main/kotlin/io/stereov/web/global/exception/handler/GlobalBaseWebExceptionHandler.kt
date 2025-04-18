package io.stereov.web.global.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.global.exception.BaseExceptionHandler
import io.stereov.web.global.exception.GlobalBaseWebException
import io.stereov.web.global.exception.model.DocumentNotFoundException
import io.stereov.web.global.exception.model.MissingFunctionParameterException
import io.stereov.web.global.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * # Exception handler for authentication exceptions.
 *
 * This class handles exceptions related to authentication operations.
 *
 * It extends the [BaseExceptionHandler] interface
 * and provides a method to handle [AuthException] and its subclasses.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ControllerAdvice
class GlobalBaseWebExceptionHandler : BaseExceptionHandler<GlobalBaseWebException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(GlobalBaseWebException::class)
    override suspend fun handleException(
        ex: GlobalBaseWebException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is MissingFunctionParameterException -> HttpStatus.INTERNAL_SERVER_ERROR
            is DocumentNotFoundException -> HttpStatus.NOT_FOUND
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
