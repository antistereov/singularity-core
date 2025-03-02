package io.stereov.web.user.model

import io.stereov.web.user.dto.DeviceInfoRequestDto
import io.stereov.web.user.dto.DeviceInfoResponseDto
import io.stereov.web.user.exception.InvalidUserDocumentException
import kotlinx.serialization.Serializable
import java.time.Instant

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
        requireNotNull(issuedAt) {
            throw InvalidUserDocumentException("Device with ID ${this.id} has no issue date")
        }

        return DeviceInfoResponseDto(
            id, browser, os, ipAddress, location, Instant.ofEpochMilli(issuedAt).toString()
        )
    }
}
