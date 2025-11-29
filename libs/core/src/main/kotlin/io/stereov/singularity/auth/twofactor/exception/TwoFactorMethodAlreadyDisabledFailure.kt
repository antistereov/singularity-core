package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.auth.twofactor.exception.TwoFactorMethodAlreadyDisabledFailure.CODE
import io.stereov.singularity.auth.twofactor.exception.TwoFactorMethodAlreadyDisabledFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a constant object that holds metadata for the "Two Factor Method Already Disabled" state.
 *
 * This object contains a predefined error code, description, and HTTP status indicating that the
 * attempt to disable a two-factor authentication method failed because it was already disabled.
 *
 * @property CODE `TWO_FACTOR_METHOD_ALREADY_DISABLED`
 * @property STATUS [HttpStatus.NOT_MODIFIED]
 */
object TwoFactorMethodAlreadyDisabledFailure {
    const val CODE = "TWO_FACTOR_METHOD_ALREADY_DISABLED"
    const val DESCRIPTION = "Two factor method is already disabled."
    val STATUS = HttpStatus.NOT_MODIFIED
}