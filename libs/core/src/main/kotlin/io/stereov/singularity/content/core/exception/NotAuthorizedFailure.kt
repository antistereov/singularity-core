package io.stereov.singularity.content.core.exception

import io.stereov.singularity.content.core.exception.NotAuthorizedFailure.CODE
import io.stereov.singularity.content.core.exception.NotAuthorizedFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a predefined failure indicating that an action is not authorized.
 *
 * Provides constant values for the failure code, description, and corresponding HTTP status,
 * which are typically used in scenarios where a user attempts to perform an operation
 * they are not authorized to carry out.
 *
 * @property CODE `NOT_AUTHORIZED`
 * @property STATUS [HttpStatus.FORBIDDEN]
 */
object NotAuthorizedFailure {
    const val CODE = "NOT_AUTHORIZED"
    const val DESCRIPTION = "Action not authorized."
    val STATUS = HttpStatus.FORBIDDEN
}