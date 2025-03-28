package io.stereov.web.global.exception

import io.stereov.web.global.model.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ServerWebExchange

/**
 * # Base interface for exception handlers.
 *
 * Interface for handling exceptions in the application.
 *
 * Implementations of this interface should provide the logic for handling exceptions.
 *
 * @see [BaseWebException]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
interface BaseExceptionHandler<T : BaseWebException> {

    /**
     * Handles the given exception and returns a [ResponseEntity] with an [ErrorResponse].
     *
     * @param ex The exception to handle.
     * @param exchange The [ServerWebExchange] for the current request.
     * @return A [ResponseEntity] containing the error response.
     */
    suspend fun handleException(ex: T, exchange: ServerWebExchange): ResponseEntity<ErrorResponse>
}
