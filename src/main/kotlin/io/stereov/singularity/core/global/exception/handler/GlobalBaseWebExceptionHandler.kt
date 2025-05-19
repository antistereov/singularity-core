package io.stereov.singularity.core.global.exception.handler

import io.stereov.singularity.core.auth.exception.AuthException
import io.stereov.singularity.core.global.exception.BaseExceptionHandler
import io.stereov.singularity.core.global.exception.GlobalBaseWebException
import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.global.exception.model.MissingFunctionParameterException
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
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    @ExceptionHandler(GlobalBaseWebException::class)
    override fun handleException(
        ex: GlobalBaseWebException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
