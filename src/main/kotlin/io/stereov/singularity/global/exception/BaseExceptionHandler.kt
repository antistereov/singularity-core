package io.stereov.singularity.global.exception

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ServerWebExchange

/**
 * # Base interface for exception handlers.
 *
 * Interface for handling exceptions in the application.
 *
 * Implementations of this interface should provide the logic for handling exceptions.
 *
 * @see [SingularityException]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
interface BaseExceptionHandler<T : SingularityException> {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun getHttpStatus(ex: T): HttpStatus
    fun handleException(ex: T, exchange: ServerWebExchange): ResponseEntity<ErrorResponse>

    /**
     * Handles the given exception and returns a [ResponseEntity] with an [ErrorResponse].
     *
     * @param ex The exception to handle.
     * @param exchange The [ServerWebExchange] for the current request.
     * @return A [ResponseEntity] containing the error response.
     */

}
