package io.stereov.web.auth.exception

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class AuthExceptionHandler {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(AuthException::class)
    suspend fun handleAuthExceptions(
        ex: AuthException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is NoTokenProvidedException -> HttpStatus.UNAUTHORIZED
            is InvalidCredentialsException -> HttpStatus.UNAUTHORIZED
            is InvalidPrincipalException -> HttpStatus.UNAUTHORIZED
            is NoTwoFactorUserAttributeException -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.UNAUTHORIZED
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
