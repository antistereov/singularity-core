package io.stereov.singularity.auth.geolocation.config

import io.stereov.singularity.auth.geolocation.exception.handler.GeoLocationExceptionHandler
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeoIpDatabaseService
import io.stereov.singularity.auth.geolocation.service.GeoLocationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
@EnableConfigurationProperties(GeolocationProperties::class)
class GeoLocationConfiguration {

    // TODO: add properties to Spring metadata

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationService(geoIpDatabaseService: GeoIpDatabaseService): GeoLocationService {
        return GeoLocationService(geoIpDatabaseService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun geoIpDatabaseService(properties: GeolocationProperties) = GeoIpDatabaseService(properties)

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationExceptionHandler() = GeoLocationExceptionHandler()
}
