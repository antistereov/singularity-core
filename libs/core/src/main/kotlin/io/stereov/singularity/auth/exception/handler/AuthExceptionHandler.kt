package io.stereov.singularity.auth.exception.handler

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.exception.AuthException
import io.stereov.singularity.auth.exception.model.*
import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.global.model.ErrorResponse
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
class AuthExceptionHandler : BaseExceptionHandler<AuthException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @ExceptionHandler(AuthException::class)
    override suspend fun handleException(
        ex: AuthException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "${ex.javaClass.simpleName} - ${ex.message}" }

        val status = when (ex) {
            is InvalidCredentialsException -> HttpStatus.UNAUTHORIZED
            is InvalidPrincipalException -> HttpStatus.UNAUTHORIZED
            is NotAuthorizedException -> HttpStatus.FORBIDDEN
            is NoTokenProvidedException -> HttpStatus.UNAUTHORIZED
            is NoTwoFactorUserAttributeException -> HttpStatus.BAD_REQUEST
            is TwoFactorAuthDisabledException -> HttpStatus.BAD_REQUEST
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
