package io.stereov.web.global.service.encryption.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.exception.BaseExceptionHandler
import io.stereov.web.global.model.ErrorResponse
import io.stereov.web.global.service.encryption.exception.EncryptionException
import io.stereov.web.global.service.encryption.exception.model.NoSecurityKeySetException
import io.stereov.web.global.service.encryption.exception.model.SecretKeyNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class EncryptionExceptionHandler : BaseExceptionHandler<EncryptionException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(EncryptionException::class)
    override suspend fun handleException(
        ex: EncryptionException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is SecretKeyNotFoundException -> HttpStatus.INTERNAL_SERVER_ERROR
            is NoSecurityKeySetException -> HttpStatus.INTERNAL_SERVER_ERROR
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
