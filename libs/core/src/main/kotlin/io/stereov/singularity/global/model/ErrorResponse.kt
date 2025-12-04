package io.stereov.singularity.global.model

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

/**
 * Represents a standardized error response returned by the application.
 *
 * This data class encapsulates details about an error that has occurred during an API request.
 * It includes information such as the timestamp of the error, HTTP status code, error code,
 * error message, and additional context like the affected request path.
 *
 * @property timestamp The date and time when the error occurred.
 * @property status The HTTP status code associated with the error.
 * @property code A unique application-specific error code identifying the type of error.
 * @property error A short description or title of the error.
 * @property message An optional detailed message providing more context about the error.
 * @property path The path of the request that triggered the error, if available.
 */
data class ErrorResponse(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val code: String,
    val error: String,
    val message: String?,
    val path: String?,
) {

    /**
     * Secondary constructor for creating an instance of [ErrorResponse] using a [SingularityException]
     * and a [ServerWebExchange]. This constructor extracts details from the provided exception and
     * exchange to populate the attributes of the error response.
     *
     * @param exception The [SingularityException] instance containing details such as the HTTP status,
     * error code, and error message.
     * @param exchange The [ServerWebExchange] instance from which the request path is obtained.
     */
    constructor(exception: SingularityException, exchange: ServerWebExchange): this(
        status = exception.status.value(),
        error = exception.javaClass.simpleName,
        message = exception.message,
        path = exchange.request.uri.path,
        code = exception.code
    )
}
