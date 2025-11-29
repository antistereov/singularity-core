package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.auth.twofactor.exception.CannotDisableOnlyTwoFactorMethodFailure.CODE
import io.stereov.singularity.auth.twofactor.exception.CannotDisableOnlyTwoFactorMethodFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a failure scenario where the last active two-factor authentication method
 * cannot be disabled. This ensures that users are not left without any two-factor
 * authentication methods, which could impact account security.
 *
 * @property CODE `CANNOT_DISABLE_ONLY_TWO_FACTOR_METHOD`
 * @property STATUS [HttpStatus.BAD_REQUEST]
 */
object CannotDisableOnlyTwoFactorMethodFailure {
    const val CODE = "CANNOT_DISABLE_ONLY_TWO_FACTOR_METHOD"
    const val DESCRIPTION = "Cannot disable only two-factor authentication method."
    val STATUS = HttpStatus.BAD_REQUEST
}