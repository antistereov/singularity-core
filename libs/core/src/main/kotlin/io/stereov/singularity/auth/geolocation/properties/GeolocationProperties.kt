package io.stereov.singularity.auth.geolocation.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.geolocation")
data class GeolocationProperties(
    val enabled: Boolean = false,
    val databaseDirectory: String = "./.data/geolocation",
    val databaseFilename: String = "GeoLite2-City.mmdb",
    val download: Boolean = true,
    val accountId: String? = null,
    val licenseKey: String? = null,
)
