package io.stereov.singularity.email.core.exception

import io.stereov.singularity.email.core.exception.EmailSendFailure.CODE
import io.stereov.singularity.email.core.exception.EmailSendFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents the metadata for email-sending failure exceptions.
 *
 * This object provides the error code, associated HTTP status, and a description
 * for exceptions related to email sending failures. It is used as a standardized
 * reference for such exceptions across the application, ensuring consistency in
 * error handling and logging.
 *
 * @property CODE `EMAIL_SEND_FAILURE`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object EmailSendFailure {
    const val CODE = "EMAIL_SEND_FAILURE"
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
    const val DESCRIPTION = "Represents an exception that occurs when an email cannot be sent."
}