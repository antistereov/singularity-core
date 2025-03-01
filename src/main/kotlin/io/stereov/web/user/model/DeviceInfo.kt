package io.stereov.web.user.model

import io.stereov.web.user.dto.DeviceInfoRequestDto
import io.stereov.web.user.dto.DeviceInfoResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val id: String,
    val tokenValue: String? = null,
    val browser: String? = null,
    val os: String? = null,
    val issuedAt: Long? = null,
    val ipAddress: String? = null,
    val location: LocationInfo? = null,
) {
    @Serializable
    data class LocationInfo(
        val latitude: Float,
        val longitude: Float,
        val cityName: String,
        val regionName: String,
        val countryCode: String,
    )

    fun toRequestDto(): DeviceInfoRequestDto {
        return DeviceInfoRequestDto(
            id = id,
            browser = browser,
            os = os,
        )
    }

    fun toResponseDto(): DeviceInfoResponseDto {
        return DeviceInfoResponseDto(
            id, browser, os, ipAddress, location
        )
    }
}
