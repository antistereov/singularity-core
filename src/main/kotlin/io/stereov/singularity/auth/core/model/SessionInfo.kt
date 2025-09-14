package io.stereov.singularity.auth.core.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

data class SessionInfo(
    val refreshTokenId: String? = null,
    val browser: String? = null,
    val os: String? = null,
    val issuedAt: Instant,
    val ipAddress: String? = null,
    val location: LocationInfo? = null
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * ## Location information model.
     *
     * This data class represents information about the location of a session.
     * It includes the latitude, longitude, city name, region name,
     * and country code.
     *
     * @property latitude The latitude of the session's location.
     * @property longitude The longitude of the session's location.
     * @property cityName The name of the city where the session is located.
     * @property countryCode The country code of the session's location.
     */
    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val cityName: String?,
        val countryCode: String,
    )
}
