package io.stereov.singularity.auth.geolocation.config

import io.stereov.singularity.auth.geolocation.exception.handler.GeolocationExceptionHandler
import io.stereov.singularity.auth.geolocation.mapper.GeolocationMapper
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeoIpDatabaseService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
@EnableConfigurationProperties(GeolocationProperties::class)
class GeoLocationConfiguration {

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationExceptionHandler() = GeolocationExceptionHandler()

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun geolocationMapper() = GeolocationMapper()

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationService(
        geoIpDatabaseService: GeoIpDatabaseService,
        properties: GeolocationProperties,
        geolocationMapper: GeolocationMapper
    ): GeolocationService {
        return GeolocationService(geoIpDatabaseService, properties, geolocationMapper)
    }

    @Bean
    @ConditionalOnMissingBean
    fun geoIpDatabaseService(properties: GeolocationProperties, webClient: WebClient) = GeoIpDatabaseService(properties, webClient)
}
