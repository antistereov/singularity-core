package io.stereov.web.user.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.user.dto.DeviceInfoRequestDto
import io.stereov.web.user.dto.DeviceInfoResponseDto
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

    fun toRequestDto(): DeviceInfoRequestDto {
        logger.debug { "Creating request dto" }

        return DeviceInfoRequestDto(
            id = id,
            browser = browser,
            os = os,
        )
    }

    fun toResponseDto(): DeviceInfoResponseDto {
        logger.debug { "Creating response dto" }

        return DeviceInfoResponseDto(
            id, browser, os, ipAddress, location, issuedAt.toString()
        )
    }
}
