package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.auth.twofactor.exception.TotpFailure.CODE
import io.stereov.singularity.auth.twofactor.exception.TotpFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a failure related to TOTP (Time-based One-Time Password) code validation.
 *
 * @property CODE `TOTP_FAILURE`
 * @property STATUS [HttpStatus.INTERNAL_SERVER_ERROR]
 */
object TotpFailure {
    const val CODE = "TOTP_FAILURE"
    const val DESCRIPTION = "TOTP code validation failed."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}