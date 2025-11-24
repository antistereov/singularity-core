package io.stereov.singularity.auth.oauth2.exception.handler

import io.stereov.singularity.auth.oauth2.exception.OAuth2Exception
import io.stereov.singularity.auth.oauth2.exception.model.CannotDisconnectIdentityProviderException
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2ProviderConnectedException
import io.stereov.singularity.auth.oauth2.exception.model.PasswordIdentityAlreadyAddedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class OAuth2ExceptionHandler : BaseExceptionHandler<OAuth2Exception> {

    override fun getHttpStatus(ex: OAuth2Exception) = when (ex) {
        is CannotDisconnectIdentityProviderException -> HttpStatus.BAD_REQUEST
        is OAuth2FlowException -> HttpStatus.BAD_REQUEST
        is OAuth2ProviderConnectedException -> HttpStatus.FORBIDDEN
        is PasswordIdentityAlreadyAddedException -> HttpStatus.NOT_MODIFIED
        else -> HttpStatus.UNAUTHORIZED
    }

    @ExceptionHandler(OAuth2Exception::class)
    override fun handleException(ex: OAuth2Exception, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)
}
