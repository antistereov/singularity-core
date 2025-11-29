package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.auth.twofactor.exception.TwoFactorMethodAlreadyEnabledFailure.CODE
import io.stereov.singularity.auth.twofactor.exception.TwoFactorMethodAlreadyEnabledFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a failure scenario where an attempt is made to enable a two-factor
 * authentication (2FA) method that is already enabled for a user or account.
 *
 * This object contains the associated failure code, description, and HTTP status
 * to be used in handling and responding to this specific error condition. It is
 * mainly used to standardize error handling related to 2FA configuration operations.
 *
 * @property CODE `TWO_FACTOR_METHOD_ALREADY_ENABLED`
 * @property STATUS [HttpStatus.NOT_MODIFIED]
 */
object TwoFactorMethodAlreadyEnabledFailure {
    const val CODE = "TWO_FACTOR_METHOD_ALREADY_ENABLED"
    const val DESCRIPTION = "Two-factor authentication method is already enabled."
    val STATUS = HttpStatus.NOT_MODIFIED
}