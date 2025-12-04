package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.principal.core.exception.TwoFactorAuthenticationDisabledFailure.CODE
import io.stereov.singularity.principal.core.exception.TwoFactorAuthenticationDisabledFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Provides constants related to the "Two-Factor Authentication Disabled" error scenario.
 *
 * This object contains information used to represent the state where
 * two-factor authentication is required for an action but has not been enabled by the user.
 * It includes constants for error handling such as an error code, description, and HTTP status.
 *
 * @property CODE `2FA_DISABLED`
 * @property STATUS [HttpStatus.BAD_REQUEST]
 */
object TwoFactorAuthenticationDisabledFailure {
    const val CODE = "2FA_DISABLED"
    const val DESCRIPTION = "User needs to set up two-factor authentication to complete this action."
    val STATUS = HttpStatus.BAD_REQUEST
}