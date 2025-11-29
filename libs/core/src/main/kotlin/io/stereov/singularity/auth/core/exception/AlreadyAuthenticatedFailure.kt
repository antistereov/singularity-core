package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.auth.core.exception.AlreadyAuthenticatedFailure.CODE
import io.stereov.singularity.auth.core.exception.AlreadyAuthenticatedFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Defines a failure representation for situations where authentication is attempted
 * but the user is already authenticated.
 *
 * This object provides constants for error code, description, and HTTP status to describe
 * the failure scenario. It can be used in conjunction with exception handling or validation
 * logic to indicate that an already authenticated principal cannot undergo further authentication.
 *
 * @property CODE `ALREADY_AUTHENTICATED`
 * @property STATUS [HttpStatus.NOT_MODIFIED]
 */
object AlreadyAuthenticatedFailure {
    const val CODE = "ALREADY_AUTHENTICATED"
    const val DESCRIPTION = "Principal is already authenticated."
    val STATUS = HttpStatus.NOT_MODIFIED
}