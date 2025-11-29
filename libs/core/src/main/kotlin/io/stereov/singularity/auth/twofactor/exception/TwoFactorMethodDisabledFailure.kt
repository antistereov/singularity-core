package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.auth.twofactor.exception.TwoFactorMethodDisabledFailure.CODE
import io.stereov.singularity.auth.twofactor.exception.TwoFactorMethodDisabledFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents the failure state when a two-factor authentication method is disabled.
 *
 * This object contains details about the failure, including the error code,
 * description, and associated HTTP status. It is often used to describe specific
 * errors encountered when two-factor authentication operations fail due to a
 * disabled method.
 *
 * @property CODE `TWO_FACTOR_METHOD_DISABLED`
 * @property STATUS [HttpStatus.BAD_REQUEST]
 */
object TwoFactorMethodDisabledFailure {
    const val CODE = "TWO_FACTOR_METHOD_DISABLED"
    const val DESCRIPTION = "Two-factor authentication method is disabled."
    val STATUS = HttpStatus.BAD_REQUEST
}