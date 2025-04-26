package io.stereov.singularity.global.service.cache.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.service.cache.exception.RedisException
import io.stereov.singularity.global.service.cache.exception.model.RedisKeyNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * # Exception handler for Redis exceptions.
 *
 * This class handles exceptions related to Redis operations.
 * It extends the [BaseExceptionHandler] interface
 * and provides a method to handle [RedisException] and its subclasses.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ControllerAdvice
class RedisExceptionHandler : BaseExceptionHandler<RedisException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(RedisException::class)
    override suspend fun handleException(
        ex: RedisException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is RedisKeyNotFoundException -> HttpStatus.NOT_FOUND
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
