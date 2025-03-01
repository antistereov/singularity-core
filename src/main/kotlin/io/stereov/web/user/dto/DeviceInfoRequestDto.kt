package io.stereov.web.user.dto

data class DeviceInfoRequestDto(
    val id: String,
    val browser: String? = null,
    val os: String? = null,
)
