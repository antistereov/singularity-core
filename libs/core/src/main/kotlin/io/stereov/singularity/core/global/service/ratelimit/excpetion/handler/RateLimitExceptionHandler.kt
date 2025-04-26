package io.stereov.singularity.core.global.service.ratelimit.excpetion.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.global.model.ErrorResponse
import io.stereov.singularity.core.global.service.ratelimit.excpetion.RateLimitException
import io.stereov.singularity.core.global.service.ratelimit.excpetion.model.TooManyRequestsException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * Handles exceptions related to rate limiting within the application.
 *
 * This handler provides a custom implementation for handling [RateLimitException] and its subclasses,
 * such as [TooManyRequestsException]. It creates and returns a suitable [ResponseEntity] containing
 * an [ErrorResponse] to be sent back to the client.
 *
 * The handler logs the details of the exception for debugging or monitoring purposes.
 *
 * Implements [BaseExceptionHandler] for seamlessly handling exceptions specific to rate limiting.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ControllerAdvice
class RateLimitExceptionHandler : BaseExceptionHandler<RateLimitException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(RateLimitException::class)
    override suspend fun handleException(
        ex: RateLimitException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is TooManyRequestsException -> HttpStatus.TOO_MANY_REQUESTS
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
