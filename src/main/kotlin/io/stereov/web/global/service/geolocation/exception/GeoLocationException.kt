package io.stereov.web.global.service.geolocation.exception

import io.stereov.web.global.exception.BaseWebException

class GeoLocationException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
