package io.stereov.singularity.email.core.exception

import io.stereov.singularity.email.core.exception.EmailTemplateFailure.CODE
import io.stereov.singularity.email.core.exception.EmailTemplateFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents the metadata for email template creation failure exceptions.
 *
 * This object provides the error code, associated HTTP status, and a description
 * for exceptions related to email template creation failures. It is used as a standardized
 * reference across the application to ensure consistency in error handling and logging.
 *
 * @property CODE `EMAIL_TEMPLATE_FAILURE`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object EmailTemplateFailure {
    const val CODE = "EMAIL_TEMPLATE_FAILURE"
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
    const val DESCRIPTION = "Thrown when there is a failure related to email template creation."
}