package io.stereov.singularity.database.core.exception

import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure.CODE
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure.STATUS
import org.springframework.http.HttpStatus

/**
 * Defines constants representing a failure to execute side effects after a successful database operation.
 *
 * This object acts as a static reference for specifying error details such as
 * the error code, description, and associated HTTP status related to post-commit
 * side effect failures.
 *
 * @property CODE `POST_COMMIT_SIDE_EFFECT_FAILURE`
 * @property STATUS [HttpStatus.MULTI_STATUS]
 */
object PostCommitSideEffectFailure {
    const val CODE = "POST_COMMIT_SIDE_EFFECT_FAILURE"
    const val DESCRIPTION = "Exception representing a failure to perform a side effect after a successful database operation."
    val STATUS = HttpStatus.MULTI_STATUS
}