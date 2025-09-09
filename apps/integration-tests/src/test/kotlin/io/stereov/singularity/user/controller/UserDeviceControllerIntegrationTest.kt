package io.stereov.singularity.user.controller

import io.stereov.singularity.auth.device.dto.DeviceInfoResponse
import io.stereov.singularity.auth.session.model.SessionTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.model.DeviceInfo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import java.time.Instant

class UserDeviceControllerIntegrationTest : BaseIntegrationTest() {

    @Test fun `getDevices returns devices`() = runTest {
        val user = registerUser(deviceId = "first")
        user.info.sensitive.devices.addAll(listOf(DeviceInfo("second", issuedAt = Instant.now()), DeviceInfo("third", issuedAt = Instant.now())))

        userService.save(user.info)

        val response = webTestClient.get()
            .uri("/api/user/devices")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(object : ParameterizedTypeReference<List<DeviceInfoResponse>>() {})
            .returnResult()
            .responseBody

        requireNotNull(response) { "No body found in response" }

        assertEquals(3, response.size)
        assertTrue(response.any { it.id == "first" })
        assertTrue(response.any { it.id == "second" })
        assertTrue(response.any { it.id == "third" })
    }
    @Test fun `getDevices requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/user/devices")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `removeDevice deletes device`() = runTest {
        val deviceId = "device"
        val user = registerUser(deviceId = deviceId)

        webTestClient.delete()
            .uri("/api/user/devices/$deviceId")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        val updatedUser = userService.findById(user.info.id)
        val devices = updatedUser.sensitive.devices

        assertEquals(0, devices.size)
    }
    @Test fun `removeDevice requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/user/devices/device")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `clearDevices deletes devices`() = runTest {
        val user = registerUser()
        webTestClient.delete()
            .uri("/api/user/devices")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus()
            .isOk

        val updatedUser = userService.findById(user.info.id)
        val devices = updatedUser.sensitive.devices

        assertEquals(0, devices.size)
    }
    @Test fun `clearDevices requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/user/devices")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
