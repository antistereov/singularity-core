package io.stereov.singularity.email.core.exception

import io.stereov.singularity.email.core.exception.EmailCooldownActiveFailure.CODE
import io.stereov.singularity.email.core.exception.EmailCooldownActiveFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents the metadata for scenarios where email sending is restricted due to an active cooldown.
 *
 * This object provides the error code, associated HTTP status, and a description for exceptions
 * that occur when an email sending request is blocked because of an enforced cooldown duration.
 * It serves as a standardized reference for such failures, ensuring consistency in error reporting
 * and handling across the application.
 *
 * @property CODE `EMAIL_COOLDOWN_ACTIVE`
 * @property STATUS [HttpStatus.TOO_MANY_REQUESTS]
 */
object EmailCooldownActiveFailure {
    const val CODE = "EMAIL_COOLDOWN_ACTIVE"
    const val DESCRIPTION = "Failed to send email because cooldown is active."
    val STATUS = HttpStatus.TOO_MANY_REQUESTS
}