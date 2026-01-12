package io.stereov.singularity.stereovio

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.global.exception.SingularityExceptionHandler
import io.stereov.singularity.global.exception.StereovIoExceptionHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class StereovWebApplication {

    @Bean
    fun singularityExceptionHandler(
        authorizationService: AuthorizationService
    ): SingularityExceptionHandler = StereovIoExceptionHandler(authorizationService)
}

fun main() {
    runApplication<StereovWebApplication>()
}
