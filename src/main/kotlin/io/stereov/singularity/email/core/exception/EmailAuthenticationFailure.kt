package io.stereov.singularity.email.core.exception

import io.stereov.singularity.email.core.exception.EmailAuthenticationFailure.CODE
import io.stereov.singularity.email.core.exception.EmailAuthenticationFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents the metadata for email authentication failure exceptions.
 *
 * This object provides the error code, associated HTTP status, and a description
 * for exceptions related to email authentication failures. It is designed to be
 * used as a standardized reference across the application wherever email
 * authentication failure needs to be represented.
 *
 * @property CODE `EMAIL_AUTHENTICATION_FAILURE`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object EmailAuthenticationFailure {
    const val CODE = "EMAIL_AUTHENTICATION_FAILURE"
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
    const val DESCRIPTION = "Thrown when there is a failure related to email authentication."
}