package io.stereov.singularity.geolocation.exception.handler

import io.stereov.singularity.global.exception.BaseExceptionHandler
import io.stereov.singularity.geolocation.exception.GeoLocationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.server.ServerWebExchange

@ControllerAdvice
class GeoLocationExceptionHandler : BaseExceptionHandler<GeoLocationException> {

    override fun getHttpStatus(ex: GeoLocationException) = when (ex) {
        else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    override fun handleException(ex: GeoLocationException, exchange: ServerWebExchange) = handleExceptionInternal(ex, exchange)

}
