package io.stereov.singularity.auth.geolocation.exception

import io.stereov.singularity.global.exception.BaseWebException

/**
 * # GeolocationException
 *
 * This class represents a custom exception for geolocation-related errors.
 * It extends the [BaseWebException] class and provides constructors to set the error message and cause.
 *
 * @param message The error message.
 * @param cause The cause of the exception (optional).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class GeolocationException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
