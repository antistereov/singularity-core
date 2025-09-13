package io.stereov.singularity.auth.oauth2.exception.handler

import io.stereov.singularity.auth.oauth2.exception.OAuth2Exception
import io.stereov.singularity.auth.oauth2.exception.model.CannotDisconnectIdentityProviderException
import io.stereov.singularity.auth.oauth2.exception.model.CannotConnectIdentityProviderException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class OAuth2ExceptionHandler : BaseExceptionHandler<OAuth2Exception> {

    override fun getHttpStatus(ex: OAuth2Exception) = when (ex) {
        is CannotConnectIdentityProviderException -> HttpStatus.BAD_REQUEST
        is CannotDisconnectIdentityProviderException -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.UNAUTHORIZED
    }

    @ExceptionHandler(OAuth2Exception::class)
    override fun handleException(ex: OAuth2Exception, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)
}
