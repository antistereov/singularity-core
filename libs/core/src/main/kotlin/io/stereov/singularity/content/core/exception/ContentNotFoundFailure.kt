package io.stereov.singularity.content.core.exception

import io.stereov.singularity.content.core.exception.ContentNotFoundFailure.CODE
import io.stereov.singularity.content.core.exception.ContentNotFoundFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Represents a predefined failure indicating that the requested content could not be found.
 *
 * This object provides constant values for the failure code, description, and corresponding
 * HTTP status, which are used in scenarios where content retrieval is attempted but
 * the content is not available or does not exist.
 *
 * @property CODE `CONTENT_NOT_FOUND`
 * @property STATUS [HttpStatus.NOT_FOUND]
 */
object ContentNotFoundFailure {
    const val CODE = "CONTENT_NOT_FOUND"
    const val DESCRIPTION = "Content not found."
    val STATUS = HttpStatus.NOT_FOUND
}