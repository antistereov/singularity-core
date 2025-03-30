package io.stereov.web.auth

import io.stereov.web.config.Constants
import io.stereov.web.test.BaseIntegrationTest
import io.stereov.web.user.dto.request.DeviceInfoRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant

class SecurityIntegrationTest : BaseIntegrationTest() {

    @Test fun `access with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `valid token required`() = runTest {
        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, "access_token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token and user account required`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `unexpired token required`() = runTest {
        val user = registerUser()
        val token = userTokenService.createAccessToken(user.info.id!!, "device", Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, token)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/logout")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/logout-all")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `invalid device will not be authorized`() = runTest {
        val user = registerUser(deviceId = "device")
        val accessToken = userTokenService.createAccessToken(user.info.idX, "device")

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.idX)
        foundUser.devices.clear()
        userService.save(foundUser)

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
