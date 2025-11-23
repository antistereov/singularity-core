package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException.Cache.Companion.CODE
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException.Cache.Companion.STATUS
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException.Expired.Companion.CODE
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException.Expired.Companion.STATUS
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException.Invalid.Companion.CODE
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException.Invalid.Companion.STATUS
import io.stereov.singularity.auth.core.model.token.AccessToken
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class AccessTokenExtractionException(
    msg: String,
    code: String,
    status: HttpStatus,
    cause: Throwable?
) : SingularityException(msg, code, status, cause) {

    /**
     * Indicates that the [AccessToken] is invalid.
     * This can be the case if:
     *
     * * the token signature is invalid,
     * * the token does not contain all necessary claims
     * * the token was blacklisted
     * *
     *
     * @param msg The error message
     * @param cause The cause
     *
     * @property CODE The error code `INVALID_ACCESS_TOKEN`
     * @property STATUS The status [HttpStatus.UNAUTHORIZED]
     */
    class Invalid(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "INVALID_ACCESS_TOKEN"
            val STATUS = HttpStatus.UNAUTHORIZED
        }
    }

    /**
     * Indicates that the [AccessToken] is expired.
     *
     * @param msg The error message
     * @param cause The cause
     *
     * @property CODE The error code `INVALID_ACCESS_TOKEN`
     * @property STATUS The status [HttpStatus.UNAUTHORIZED]
     */
    class Expired(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "ACCESS_TOKEN_EXPIRED"
            val STATUS = HttpStatus.UNAUTHORIZED
        }
    }

    /**
     * Represents an exception indicating a failure when reading the allowlist for an access token cache.
     *
     * @param msg The error message describing the cause of the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property CODE The error code `ACCESS_TOKEN_ALLOWLIST_READING_FAILURE` associated with this exception
     * @property STATUS The status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Cache(msg: String, cause: Throwable? = null) : AccessTokenExtractionException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "ACCESS_TOKEN_ALLOWLIST_READING_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
