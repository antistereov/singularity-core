package io.stereov.singularity.core.global.model

import java.time.LocalDateTime

/**
 * # ErrorResponse
 *
 * This data class represents the structure of an error response returned by the application.
 *
 * @property timestamp The timestamp when the error occurred.
 * @property status The HTTP status code of the error.
 * @property error A brief description of the error.
 * @property message A detailed error message, if available.
 * @property path The path of the request that caused the error.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String?,
)
