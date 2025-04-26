package io.stereov.singularity.user.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.user.exception.UserException
import io.stereov.singularity.user.exception.model.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

/**
 * # Global exception handler for user-related exceptions.
 *
 * This class handles exceptions related to user operations and returns appropriate HTTP responses.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
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
            is InvalidRoleException -> HttpStatus.INTERNAL_SERVER_ERROR
            is NoAppInfoFoundException -> HttpStatus.NOT_FOUND
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
