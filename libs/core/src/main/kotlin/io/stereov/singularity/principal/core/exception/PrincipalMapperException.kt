package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.model.Principal
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to [Principal] mapping operations.
 *
 * This sealed class serves as a base for specific exceptions that may occur
 * during the processes of mapping user-related data, such as avatars or documents.
 * It extends [SingularityException] to enforce a consistent structure for capturing
 * error details, including a message, unique error code, HTTP status, description,
 * and optionally the underlying cause.
 *
 * @param msg The error message providing context for the exception.
 * @param code A unique code identifying the specific type of user mapping error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class PrincipalMapperException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing a failure to map avatar metadata for a user.
     *
     * This exception is a specific subtype of [PrincipalMapperException] and is thrown when
     * the system encounters an error during the process of mapping or handling user avatar metadata.
     *
     * @param msg A message providing details about the avatar mapping error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `USER_AVATAR_MAPPING_FAILURE`
     * @property status [HttpStatus.MULTI_STATUS]
     */
    class Avatar(msg: String, cause: Throwable? = null) : PrincipalMapperException(
        msg,
        "USER_AVATAR_MAPPING_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Failed to map avatar metadata.",
        cause
    )

    /**
     * Exception representing an invalid user document error.
     *
     * This exception is thrown when the system encounters an invalid user document
     * during the mapping process. It provides specific details about the failure and
     * is a subtype of [PrincipalMapperException].
     *
     * @param msg A message describing the details of the invalid document error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see InvalidPrincipalDocumentFailure
     */
    class InvalidDocument(msg: String, cause: Throwable? = null) : PrincipalMapperException(
        msg,
        InvalidPrincipalDocumentFailure.CODE,
        InvalidPrincipalDocumentFailure.STATUS,
        InvalidPrincipalDocumentFailure.DESCRIPTION,
        cause
    )
}
