package io.stereov.singularity.ratelimit.excpetion.handler

import io.stereov.singularity.ratelimit.excpetion.RateLimitException
import io.stereov.singularity.ratelimit.excpetion.model.TooManyRequestsException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.global.model.ErrorResponse
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

    override fun getHttpStatus(ex: RateLimitException) = when (ex) {
        is TooManyRequestsException -> HttpStatus.TOO_MANY_REQUESTS
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(RateLimitException::class)
    override fun handleException(
        ex: RateLimitException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
