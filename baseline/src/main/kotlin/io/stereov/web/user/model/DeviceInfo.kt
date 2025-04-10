package io.stereov.web.user.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.user.dto.request.DeviceInfoRequest
import io.stereov.web.user.dto.response.DeviceInfoResponse
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * # Device information model.
 *
 * This data class represents information about a device used for authentication.
 * It includes the device ID, token value, browser and OS information, issued time,
 * IP address, and location information.
 *
 * @property id The ID of the device.
 * @property refreshTokenId The refresh token value associated with the device.
 * @property browser The browser used on the device.
 * @property os The operating system of the device.
 * @property issuedAt The time when the device information was issued.
 * @property ipAddress The IP address of the device.
 * @property location The location information of the device.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class DeviceInfo(
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
     * This data class represents information about the location of a device.
     * It includes the latitude, longitude, city name, region name,
     * and country code.
     *
     * @property latitude The latitude of the device's location.
     * @property longitude The longitude of the device's location.
     * @property cityName The name of the city where the device is located.
     * @property regionName The name of the region where the device is located.
     * @property countryCode The country code of the device's location.
     */
    @Serializable
    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val cityName: String,
        val regionName: String,
        val countryCode: String,
    )

    /**
     * Converts the device information to a request DTO.
     *
     * This function creates a [DeviceInfoRequest] object from the current
     * device information instance. It is used for sending device information
     * in requests.
     *
     * @return A [DeviceInfoRequest] object containing the device information.
     */
    fun toRequestDto(): DeviceInfoRequest {
        logger.debug { "Creating request dto" }

        return DeviceInfoRequest(
            id = id,
            browser = browser,
            os = os,
        )
    }

    /**
     * Converts the device information to a response DTO.
     *
     * This function creates a [DeviceInfoResponse] object from the current
     * device information instance. It is used for sending device information
     * in responses.
     *
     * @return A [DeviceInfoResponse] object containing the device information.
     */
    fun toResponseDto(): DeviceInfoResponse {
        logger.debug { "Creating response dto" }

        return DeviceInfoResponse(
            id, browser, os, ipAddress, location, issuedAt.toString()
        )
    }
}
