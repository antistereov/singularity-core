package io.stereov.singularity.email.core.exception

import io.stereov.singularity.email.core.exception.EmailDisabledFailure.CODE
import io.stereov.singularity.email.core.exception.EmailDisabledFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Contains metadata for email functionality being disabled.
 *
 * This object provides the error code, associated HTTP status, and description
 * for exceptions that occur when email functionality is disabled in the application.
 * It is used as a standardized reference for such exceptions.
 *
 * @property CODE `EMAIL_DISABLED`
 * @property STATUS [HttpStatus.SERVICE_UNAVAILABLE]
 */
object EmailDisabledFailure {
    const val CODE = "EMAIL_DISABLED"
    const val DESCRIPTION = "Thrown when email functionality is disabled in the application."
    val STATUS = HttpStatus.SERVICE_UNAVAILABLE
}