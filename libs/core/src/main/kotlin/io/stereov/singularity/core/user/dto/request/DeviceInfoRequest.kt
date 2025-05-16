package io.stereov.singularity.core.user.dto.request

/**
 * # DeviceInfoRequest data class.
 *
 * This data class represents a request for device information.
 * It includes the device ID, browser, and operating system.
 *
 * @property id The ID of the device.
 * @property browser The browser used on the device (optional).
 * @property os The operating system of the device (optional).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class DeviceInfoRequest(
    val id: String,
    val browser: String? = null,
    val os: String? = null,
)
