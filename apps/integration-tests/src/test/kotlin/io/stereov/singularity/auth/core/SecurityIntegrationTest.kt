package io.stereov.singularity.auth.core.core

import io.stereov.singularity.auth.device.dto.DeviceInfoRequest
import io.stereov.singularity.auth.jwt.service.JwtSecretService
import io.stereov.singularity.auth.session.dto.response.RefreshTokenResponse
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant

class SecurityIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var jwtDecoder: ReactiveJwtDecoder

    @Autowired
    private lateinit var jwtSecretService: JwtSecretService

    @Test fun `access with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `valid token required`() = runTest {
        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, "access_token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token and user account required`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `unexpired token required`() = runTest {
        val user = registerUser()
        val token = accessTokenService.createAccessToken(user.info.id, "device", Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, token)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/logout")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/logout-all")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `invalid device will not be authorized`() = runTest {
        val user = registerUser(deviceId = "device")
        val accessToken = accessTokenService.createAccessToken(user.info.id, "device")

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.id)
        foundUser.sensitive.devices.clear()
        userService.save(foundUser)

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `refresh works`() = runTest {
        val user = registerUser(deviceId = "device")

        val response = webTestClient.post()
            .uri("/api/user/refresh")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, response.accessToken!!)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh does not invalidate old token`() = runTest {
        val user = registerUser(deviceId = "device")

        val response = webTestClient.post()
            .uri("/api/user/refresh")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, response.accessToken!!)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh requires valid token`() = runTest {
        val user = registerUser(deviceId = "device")

        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, "invalid-token")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires unexpired token`() = runTest {
        val user = registerUser(deviceId = "device")
        val token = accessTokenService.createRefreshToken(user.info.id, "device", user.info.sensitive.devices.first().refreshTokenId!!,Instant.ofEpochSecond(0))

        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, token)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires token and user account`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/logout")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/logout-all")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh invalid device will not be authorized`() = runTest {
        val user = registerUser(deviceId = "device")
        val accessToken = accessTokenService.createAccessToken(user.info.id, "device")

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.id)
        foundUser.sensitive.devices.clear()
        userService.save(foundUser)

        webTestClient.post()
            .uri("/api/user/refresh")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `key rotation works`() = runTest {
        val user = registerUser()

        jwtSecretService.updateSecret()

        val newUser = registerUser("another@email.com")

        val newJwt = jwtDecoder.decode(newUser.accessToken).awaitFirst()
        val newKeyId = newJwt.headers["kid"]


        val jwt = jwtDecoder.decode(user.accessToken).awaitFirst()
        val oldKeyId = jwt.headers["kid"]

        assertNotEquals(oldKeyId, newKeyId)
    }


}
