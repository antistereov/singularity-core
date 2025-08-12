package io.stereov.singularity.auth.geolocation.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.geolocation")
data class GeolocationProperties(
    val enabled: Boolean = false,
    val databaseDirectory: String = "./.data/geolocation",
    val licenseKey: String? = null,
    val user: String? = null,
)
