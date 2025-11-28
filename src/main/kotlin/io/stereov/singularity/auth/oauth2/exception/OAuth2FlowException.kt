package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents an exception thrown specifically during the OAuth2 authentication or authorization flow.
 *
 * This exception extends [SingularityException] and provides more detailed context regarding failures
 * that occur during an OAuth2 flow. It encapsulates an [OAuth2ErrorCode] that identifies the specific error
 * condition, allowing for more granular error handling and debugging.
 *
 * @param errorCode The specific type of OAuth2 error associated with this exception, represented by an [OAuth2ErrorCode].
 * @param msg A descriptive message providing details about the error.
 * @param cause The underlying cause of the exception, if available.
 */
class OAuth2FlowException(
    val errorCode: OAuth2ErrorCode,
    msg: String,
    cause: Throwable? = null
) : SingularityException(
    msg,
    errorCode.value,
    HttpStatus.BAD_REQUEST,
    "Thrown when an OAuth2 flow fails due to an error in the request.",
    cause
)