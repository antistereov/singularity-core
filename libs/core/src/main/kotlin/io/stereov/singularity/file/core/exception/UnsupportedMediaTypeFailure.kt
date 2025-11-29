package io.stereov.singularity.file.core.exception

import io.stereov.singularity.file.core.exception.UnsupportedMediaTypeFailure.CODE
import io.stereov.singularity.file.core.exception.UnsupportedMediaTypeFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents predefined information related to the "Unsupported Media Type" failure scenario.
 *
 * This object defines constants and properties that provide details about the error,
 * including the specific error code, description, and HTTP status. It is used
 * to standardize and centralize error data for scenarios where a request is made
 * with a media type that is not supported.
 *
 * @property CODE `UNSUPPORTED_MEDIA_TYPE`
 * @property STATUS [HttpStatus.UNSUPPORTED_MEDIA_TYPE]
 */
object UnsupportedMediaTypeFailure {
    const val CODE = "UNSUPPORTED_MEDIA_TYPE"
    const val DESCRIPTION = "Thrown when a request is made with an unsupported media type."
    val STATUS = HttpStatus.UNSUPPORTED_MEDIA_TYPE
}