package io.stereov.singularity.auth.geolocation.config

import io.stereov.singularity.auth.geolocation.exception.handler.GeoLocationExceptionHandler
import io.stereov.singularity.auth.geolocation.service.GeoLocationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
class GeoLocationConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationService(webClient: WebClient): GeoLocationService {
        return GeoLocationService(webClient)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationExceptionHandler() = GeoLocationExceptionHandler()
}
