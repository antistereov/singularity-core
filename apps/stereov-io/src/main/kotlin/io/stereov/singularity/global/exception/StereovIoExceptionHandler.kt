package io.stereov.singularity.global.exception

import com.github.michaelbull.result.get
import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry
import io.sentry.protocol.User
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.global.model.ErrorResponse
import jakarta.annotation.PostConstruct
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebExchange

class StereovIoExceptionHandler(
    private val authorizationService: AuthorizationService
) : SingularityExceptionHandler() {

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun init() {
        logger.info { "Using custom error handler: StereovIoExceptionHandler" }
    }

    @ExceptionHandler(SingularityException::class)
    @Suppress("UNUSED")
    override suspend fun handleException(ex: SingularityException, exchange: ServerWebExchange): ResponseEntity<ErrorResponse> {
        if (ex.status.is5xxServerError) {
            val userId = authorizationService.getAuthenticationOutcome().get()
                ?.principal

            Sentry.captureException(ex) { scope ->
                scope.user = User().apply {
                    id = userId.toString()
                }
                scope.setTag("path", exchange.request.path.toString())
            }
        }

        return super.handleException(ex, exchange)
    }
}