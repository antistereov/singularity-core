package io.stereov.singularity.global.exception.handler

import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.global.exception.GlobalBaseWebException
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.exception.model.MissingFunctionParameterException
import io.stereov.singularity.global.exception.model.MissingRequestParameterException
import org.springframework.http.HttpStatus
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

    override fun getHttpStatus(ex: GlobalBaseWebException) = when (ex) {
        is MissingFunctionParameterException -> HttpStatus.INTERNAL_SERVER_ERROR
        is DocumentNotFoundException -> HttpStatus.NOT_FOUND
        is InvalidDocumentException -> HttpStatus.INTERNAL_SERVER_ERROR
        is MissingRequestParameterException -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(GlobalBaseWebException::class)
    override fun handleException(
        ex: GlobalBaseWebException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
