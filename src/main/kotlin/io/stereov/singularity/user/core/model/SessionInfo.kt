package io.stereov.singularity.user.core.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import java.time.Instant

/**
 * # session information model.
 *
 * This data class represents information about a session used for authentication.
 * It includes the session ID, token value, browser and OS information, issued time,
 * IP address, and location information.
 *
 * @property id The ID of the session.
 * @property refreshTokenId The refresh token value associated with the session.
 * @property browser The browser used on the session.
 * @property os The operating system of the session.
 * @property issuedAt The time when the session information was issued.
 * @property ipAddress The IP address of the session.
 * @property location The location information of the session.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class SessionInfo(
    val id: String,
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

    /**
     * Converts the session information to a request DTO.
     *
     * This function creates a [SessionInfoRequest] object from the current
     * session information instance. It is used for sending session information
     * in requests.
     *
     * @return A [SessionInfoRequest] object containing the session information.
     */
    fun toRequestDto(): SessionInfoRequest {
        logger.debug { "Creating request dto" }

        return SessionInfoRequest(
            id = id,
            browser = browser,
            os = os,
        )
    }

    /**
     * Converts the session information to a response DTO.
     *
     * This function creates a [SessionInfoResponse] object from the current
     * session information instance. It is used for sending session information
     * in responses.
     *
     * @return A [SessionInfoResponse] object containing the session information.
     */
    fun toResponseDto(): SessionInfoResponse {
        logger.debug { "Creating response dto" }

        return SessionInfoResponse(
            id, browser, os, ipAddress, location, issuedAt
        )
    }
}
