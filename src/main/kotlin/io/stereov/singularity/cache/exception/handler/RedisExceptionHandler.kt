package io.stereov.singularity.cache.exception.handler

import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.cache.exception.RedisException
import io.stereov.singularity.cache.exception.model.RedisKeyNotFoundException
import org.springframework.http.HttpStatus
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

    override fun getHttpStatus(ex: RedisException) = when (ex) {
        is RedisKeyNotFoundException -> HttpStatus.NOT_FOUND
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(RedisException::class)
    override fun handleException(
        ex: RedisException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
