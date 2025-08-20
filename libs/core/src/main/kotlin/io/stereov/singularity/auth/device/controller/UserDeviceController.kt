package io.stereov.singularity.auth.device.controller

import io.stereov.singularity.auth.device.dto.DeviceInfoResponse
import io.stereov.singularity.auth.device.service.UserDeviceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * # UserDeviceController class.
 *
 * This class handles HTTP requests related to user devices.
 * It provides endpoints to get the list of devices,
 * remove a specific device, and clear all devices.
 *
 * It uses the [UserDeviceService] to perform operations on user devices.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@RestController
@RequestMapping("/api/user/devices")
class UserDeviceController(
    private val deviceService: UserDeviceService
) {

    /**
     * Gets the list of devices associated with the current user.
     *
     * @return A [ResponseEntity] containing a list of [DeviceInfoResponse] objects representing the user's devices.
     */
    @GetMapping
    suspend fun getDevices(): ResponseEntity<List<DeviceInfoResponse>> {
        val devices = deviceService.getDevices()

        return ResponseEntity.ok(devices.map { it.toResponseDto() })
    }

    /**
     * Removes a device from the current user's account.
     *
     * @param deviceId The ID of the device to remove.
     *
     * @return A [ResponseEntity] containing a list of [DeviceInfoResponse] objects representing the updated user's devices.
     */
    @DeleteMapping("/{deviceId}")
    suspend fun removeDevice(@PathVariable deviceId: String): ResponseEntity<List<DeviceInfoResponse>> {
        val updatedUser = deviceService.removeDevice(deviceId)

        return ResponseEntity.ok(updatedUser.sensitive.devices.map { it.toResponseDto()})
    }

    /**
     * Clears all devices from the current user's account.
     *
     * @return A [ResponseEntity] indicating the success of the operation.
     */
    @DeleteMapping
    suspend fun clearDevices(): ResponseEntity<Map<String, Boolean>> {
        deviceService.clearDevices()

        return ResponseEntity.ok()
            .body(mapOf("success" to true))
    }
}
