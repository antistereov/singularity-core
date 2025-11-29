package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.auth.core.exception.InvalidCredentialsFailure.CODE
import io.stereov.singularity.auth.core.exception.InvalidCredentialsFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a failure scenario where provided authentication credentials are invalid.
 *
 * @property CODE `INVALID_CREDENTIALS`
 * @property STATUS [HttpStatus.UNAUTHORIZED]
 */
object InvalidCredentialsFailure {
    const val CODE = "INVALID_CREDENTIALS"
    const val DESCRIPTION = "Invalid credentials."
    val STATUS = HttpStatus.UNAUTHORIZED
}