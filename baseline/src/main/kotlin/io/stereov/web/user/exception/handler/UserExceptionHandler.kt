package io.stereov.web.user.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.model.ErrorResponse
import io.stereov.web.user.exception.*
import io.stereov.web.user.exception.model.EmailAlreadyExistsException
import io.stereov.web.user.exception.model.InvalidRoleException
import io.stereov.web.user.exception.model.InvalidUserDocumentException
import io.stereov.web.user.exception.model.UserDoesNotExistException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class UserExceptionHandler {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(UserException::class)
    suspend fun handleAccountExceptions(
        ex: UserException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is UserDoesNotExistException -> HttpStatus.UNAUTHORIZED
            is EmailAlreadyExistsException -> HttpStatus.CONFLICT
            is InvalidUserDocumentException -> HttpStatus.INTERNAL_SERVER_ERROR
            is InvalidRoleException -> HttpStatus.INTERNAL_SERVER_ERROR
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
