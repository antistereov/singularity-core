package io.stereov.singularity.auth.core.model

import java.time.Instant

data class SessionInfo(
    val refreshTokenId: String? = null,
    val browser: String? = null,
    val os: String? = null,
    val issuedAt: Instant,
    val ipAddress: String? = null,
    val location: LocationInfo? = null
) {

    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val cityName: String?,
        val countryCode: String,
    )
}
