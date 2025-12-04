package io.stereov.singularity.email.core.exception

import io.stereov.singularity.email.core.exception.EmailCooldownCacheFailure.CODE
import io.stereov.singularity.email.core.exception.EmailCooldownCacheFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents the metadata for email cooldown cache failure exceptions.
 *
 * This object provides the error code, associated HTTP status, and a description
 * for exceptions that occur when an error arises while setting or retrieving
 * cooldown-related data. It serves as a standardized reference for such failures,
 * ensuring consistent error reporting and handling across the application.
 *
 * @property CODE `EMAIL_COOLDOWN_CACHE_FAILURE`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object EmailCooldownCacheFailure {
    const val CODE = "EMAIL_COOLDOWN_CACHE_FAILURE"
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
    const val DESCRIPTION = "Thrown when an exception occurs when setting or getting cooldown."
}