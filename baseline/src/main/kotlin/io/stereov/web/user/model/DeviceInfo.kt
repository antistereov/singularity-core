package io.stereov.web.user.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.user.dto.DeviceInfoRequest
import io.stereov.web.user.dto.DeviceInfoResponse
import kotlinx.serialization.Serializable
import java.time.Instant

data class DeviceInfo(
    val id: String,
    val tokenValue: String? = null,
    val browser: String? = null,
    val os: String? = null,
    val issuedAt: Instant,
    val ipAddress: String? = null,
    val location: LocationInfo? = null
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @Serializable
    data class LocationInfo(
        val latitude: Float,
        val longitude: Float,
        val cityName: String,
        val regionName: String,
        val countryCode: String,
    )

    fun toRequestDto(): DeviceInfoRequest {
        logger.debug { "Creating request dto" }

        return DeviceInfoRequest(
            id = id,
            browser = browser,
            os = os,
        )
    }

    fun toResponseDto(): DeviceInfoResponse {
        logger.debug { "Creating response dto" }

        return DeviceInfoResponse(
            id, browser, os, ipAddress, location, issuedAt.toString()
        )
    }
}
