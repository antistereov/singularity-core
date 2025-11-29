package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.auth.twofactor.exception.TwoFactorCodeExpiredFailure.CODE
import io.stereov.singularity.auth.twofactor.exception.TwoFactorCodeExpiredFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a failure caused by the expiration of a two-factor authentication code.
 *
 * This object encapsulates the error details associated with an expired two-factor code,
 * including the specific error code, description, and HTTP status.
 *
 * @property CODE `TWO_FACTOR_CODE_EXPIRED`
 * @property STATUS [HttpStatus.UNAUTHORIZED]
 */
object TwoFactorCodeExpiredFailure {
    const val CODE = "TWO_FACTOR_CODE_EXPIRED"
    const val DESCRIPTION = "Two-factor code has expired."
    val STATUS = HttpStatus.UNAUTHORIZED
}