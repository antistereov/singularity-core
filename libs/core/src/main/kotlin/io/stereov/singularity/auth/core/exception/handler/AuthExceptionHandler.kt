package io.stereov.singularity.auth.core.exception.handler

import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.*
import io.stereov.singularity.global.exception.BaseExceptionHandler
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
class AuthExceptionHandler : BaseExceptionHandler<AuthException> {

    override fun getHttpStatus(ex: AuthException) = when (ex) {
        is EmailAlreadyVerifiedException -> HttpStatus.NOT_MODIFIED
        is InvalidCredentialsException -> HttpStatus.UNAUTHORIZED
        is InvalidPrincipalException -> HttpStatus.UNAUTHORIZED
        is NotAuthorizedException -> HttpStatus.FORBIDDEN
        is NoTokenProvidedException -> HttpStatus.UNAUTHORIZED
        is NoTwoFactorUserAttributeException -> HttpStatus.BAD_REQUEST
        is TwoFactorMethodDisabledException -> HttpStatus.BAD_REQUEST
        is UserAlreadyAuthenticatedException -> HttpStatus.NOT_MODIFIED
        is WrongIdentityProviderException -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.UNAUTHORIZED
    }

    @ExceptionHandler(AuthException::class)
    override fun handleException(
        ex: AuthException,
        exchange: ServerWebExchange
    ) = handleExceptionInternal(ex, exchange)
}
