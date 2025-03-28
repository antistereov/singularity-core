package io.stereov.web.user.controller

import io.stereov.web.user.dto.DeviceInfoResponse
import io.stereov.web.user.service.device.UserDeviceService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/user/devices")
class UserDeviceController(
    private val deviceService: UserDeviceService
) {

    @GetMapping
    suspend fun getDevices(): ResponseEntity<List<DeviceInfoResponse>> {
        val devices = deviceService.getDevices()

        return ResponseEntity.ok(devices.map { it.toResponseDto() })
    }

    @DeleteMapping("/{deviceId}")
    suspend fun removeDevice(@PathVariable deviceId: String): ResponseEntity<List<DeviceInfoResponse>> {
        val updatedUser = deviceService.removeDevice(deviceId)

        return ResponseEntity.ok(updatedUser.devices.map { it.toResponseDto()})
    }

    @DeleteMapping
    suspend fun clearDevices(): ResponseEntity<Map<String, Boolean>> {
        deviceService.clearDevices()

        return ResponseEntity.ok()
            .body(mapOf("success" to true))
    }
}
