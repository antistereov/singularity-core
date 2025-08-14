package io.stereov.singularity.auth.geolocation.exception.handler

import io.stereov.singularity.auth.geolocation.exception.GeolocationException
import io.stereov.singularity.global.exception.BaseExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class GeolocationExceptionHandler : BaseExceptionHandler<GeolocationException> {

    override fun getHttpStatus(ex: GeolocationException) = when (ex) {
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    override fun handleException(ex: GeolocationException, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)

}
