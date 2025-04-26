package io.stereov.singularity.core.global.service.geolocation.exception

import io.stereov.singularity.core.global.exception.BaseWebException

/**
 * # GeoLocationException
 *
 * This class represents a custom exception for geolocation-related errors.
 * It extends the [BaseWebException] class and provides constructors to set the error message and cause.
 *
 * @param message The error message.
 * @param cause The cause of the exception (optional).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class GeoLocationException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
