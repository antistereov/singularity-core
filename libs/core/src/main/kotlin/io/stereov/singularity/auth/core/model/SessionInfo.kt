package io.stereov.singularity.auth.core.model

import java.time.Instant

/**
 * Represents information about a session for a user.
 *
 * This data class stores details of a user's session, including information
 * such as the browser and operating system used, the IP address, location,
 * and the time the session was issued. Session data can be utilized for
 * security monitoring, session management, and user activity tracking.
 *
 * @property refreshTokenId The identifier for the refresh token associated with the session.
 * @property browser The name or type of browser used for the session.
 * @property os The operating system of the device used for the session.
 * @property issuedAt The timestamp indicating when the session was created or issued.
 * @property ipAddress The IP address of the user at the time of the session.
 * @property location The geographical location associated with the session, encapsulated
 * within the [LocationInfo] data class.
 */
data class SessionInfo(
    val refreshTokenId: String? = null,
    val browser: String? = null,
    val os: String? = null,
    val issuedAt: Instant = Instant.now(),
    val ipAddress: String? = null,
    val location: LocationInfo? = null
) {

    /**
     * Represents geographical location information.
     *
     * This data class encapsulates details about a specific location, including
     * its geographical coordinates, city name, and country code. It is used to
     * provide contextual information about locations associated with user sessions
     * or other geographically relevant data in the system.
     *
     * @property latitude The latitude coordinate of the location.
     * @property longitude The longitude coordinate of the location.
     * @property cityName The name of the city for the location, or null if unavailable.
     * @property countryCode The ISO code of the country for the location.
     */
    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val cityName: String?,
        val countryCode: String,
    )
}
