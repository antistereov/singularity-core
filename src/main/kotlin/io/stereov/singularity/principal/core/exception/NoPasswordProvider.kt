package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.principal.core.exception.NoPasswordProvider.CODE
import io.stereov.singularity.principal.core.exception.NoPasswordProvider.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a default provider for cases when no password-based authentication is configured.
 *
 * This object is used in scenarios where a user attempts an action that
 * requires the presence of password-based authentication, but this type of
 * authentication has not been set up. It provides constant values for error handling
 * and communication, such as an error code, description, and HTTP status.
 *
 * @property CODE `NO_PASSWORD_PROVIDER`
 * @property STATUS [HttpStatus.BAD_REQUEST]
 */
object NoPasswordProvider {
    const val CODE = "NO_PASSWORD_PROVIDER"
    const val DESCRIPTION = "The user needs to set a password in to perform this action."
    val STATUS = HttpStatus.BAD_REQUEST
}