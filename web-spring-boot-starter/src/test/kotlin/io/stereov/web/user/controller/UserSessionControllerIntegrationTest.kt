package io.stereov.web.user.controller

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.web.BaseIntegrationTest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import io.stereov.web.config.Constants
import io.stereov.web.user.dto.*
import io.stereov.web.user.model.DeviceInfo
import org.junit.jupiter.api.Assertions.*

class UserSessionControllerIntegrationTest : BaseIntegrationTest() {

    val gAuth = GoogleAuthenticator()

    @Test fun `getAccount returns user account`() = runTest {
        val user = registerUser()

        val responseBody = webTestClient.get()
            .uri("/user/me")
            .header(HttpHeaders.COOKIE, "${Constants.ACCESS_TOKEN_COOKIE}=${user.accessToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(responseBody) { "Response has empty body" }

        assertEquals(user.info.email, responseBody.email)
    }
    @Test fun `getAccount needs authentication`() = runTest {
        webTestClient.get()
            .uri("/user/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `login logs in user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)
        val loginRequest = LoginRequest(email, password, DeviceInfoRequestDto(deviceId))

        val response = webTestClient.post()
            .uri("/user/login")
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val accessToken = response.responseCookies[Constants.ACCESS_TOKEN_COOKIE]
            ?.firstOrNull()?.value
        val refreshToken = response.responseCookies[Constants.REFRESH_TOKEN_COOKIE]
            ?.firstOrNull()?.value
        val account = response.responseBody?.user

        requireNotNull(accessToken) { "No access token provided in response" }
        requireNotNull(refreshToken) { "No refresh token provided in response" }
        requireNotNull(account) { "No auth info provided in response" }

        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())
        assertEquals(user.info.id, account.id)

        val userDto = webTestClient.get()
            .uri("user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userDto)

        assertEquals(user.info.id, userDto.id)
        assertEquals(user.info.email, userDto.email)

        assertEquals(1, userService.findAll().count())
    }
    @Test fun `login needs body`() = runTest {
        webTestClient.post()
            .uri("/user/login")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `login needs valid credentials`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/login")
            .bodyValue(LoginRequest(user.info.email, "wrong password", user.info.devices.first().toRequestDto()))
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/user/login")
            .bodyValue(LoginRequest("another@email.com", "wrong password", user.info.devices.first().toRequestDto()))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `login from new device saves device`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val newDeviceId = "newDeviceId"

        registerUser(email, password, deviceId)

        val accessToken = webTestClient.post()
            .uri("/user/login")
            .bodyValue(LoginRequest(email, password, DeviceInfoRequestDto(newDeviceId)))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token provided in response" }

        val userInfo = webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userInfo) { "No UserDetails provided in response" }

        val devices = userInfo.devices

        assertEquals(2, devices.size)
        assertTrue(devices.any { it.id == deviceId })
        assertTrue(devices.any { it.id == newDeviceId })
    }

    @Test fun `login with two factor returns no user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"


        val user = registerUser(email, password, deviceId, true)

        val loginRequest = LoginRequest(email, password, DeviceInfoRequestDto(deviceId))

        val response = webTestClient.post()
            .uri("/user/login")
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        assertTrue(response.twoFactorRequired)
        assertNull(response.user)
    }

    @Test fun `register registers new user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val deviceInfo = DeviceInfoRequestDto(id = deviceId)

        val response = webTestClient.post()
            .uri("/user/register")
            .bodyValue(RegisterUserDto(email = email, password = password, device = deviceInfo))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val accessToken = response.responseCookies[Constants.ACCESS_TOKEN_COOKIE]
            ?.firstOrNull()?.value
        val refreshToken = response.responseCookies[Constants.REFRESH_TOKEN_COOKIE]
            ?.firstOrNull()?.value
        val userDto = response.responseBody

        requireNotNull(accessToken) { "No access token provided in response" }
        requireNotNull(refreshToken) { "No refresh token provided in response" }
        requireNotNull(userDto) { "No user info provided in response" }

        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())

        val userDetails = webTestClient.get()
            .uri("user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userDetails) { "No UserDetails provided in response" }

        assertEquals(userDto.id, userDetails.id)
        assertEquals(1, userDetails.devices.size)
        assertEquals(deviceId, userDetails.devices.first().id)

        assertEquals(1, userService.findAll().count())
    }
    @Test fun `register requires valid credentials`() = runTest {
        val deviceInfo = DeviceInfoRequestDto("device")
        webTestClient.post()
            .uri("/user/register")
            .bodyValue(RegisterUserDto(email = "invalid", password = "password", device = deviceInfo))
            .exchange()
            .expectStatus().isBadRequest

        webTestClient.post()
            .uri("/user/register")
            .bodyValue(RegisterUserDto(email = "", password = "password", device = deviceInfo))
            .exchange()
            .expectStatus().isBadRequest

        webTestClient.post()
            .uri("/user/register")
            .bodyValue(RegisterUserDto(email = "test@email.com", password = "", device = deviceInfo))
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register needs body`() = runTest {
        webTestClient.post()
            .uri("/user/login")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `getDevices returns devices`() = runTest {
        val user = registerUser(deviceId = "first")
        val devices = user.info.devices.toMutableList()
        devices.addAll(listOf(DeviceInfo("second"), DeviceInfo("third")))

        userService.save(user.info.copy(
            devices = devices
        ))

        val response = webTestClient.get()
            .uri("/user/devices")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(object : ParameterizedTypeReference<Map<String, List<DeviceInfoRequestDto>>>() {})
            .returnResult()
            .responseBody

        requireNotNull(response) { "No body found in response" }

        val deviceResponse = response["devices"]

        requireNotNull(deviceResponse) { "No device key found in response" }

        assertEquals(3, deviceResponse.size)
        assertTrue(deviceResponse.any { it.id == "first" })
        assertTrue(deviceResponse.any { it.id == "second" })
        assertTrue(deviceResponse.any { it.id == "third" })
    }
    @Test fun `getDevices requires authentication`() = runTest {
        webTestClient.get()
            .uri("/user/devices")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `deleteDevice deletes device`() = runTest {
        val deviceId = "device"
        val user = registerUser(deviceId = deviceId)

        webTestClient.delete()
            .uri("/user/devices?device_id=$deviceId")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        val updatedUser = userService.findById(user.info.id!!)
        val devices = updatedUser.devices

        assertEquals(0, devices.size)
    }
    @Test fun `deleteDevice requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/user/devices?device_id=device")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `deleteDevice requires request param`() = runTest {
        val user = registerUser()
        webTestClient.delete()
            .uri("/user/devices")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `checkAuthentication requires authentication`() = runTest {
        webTestClient.get()
            .uri("/user/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `checkAuthentication returns user`() = runTest {
        val user = registerUser()

        val response = webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response) { "Response body is empty" }

        assertEquals(user.info.id, response.id)
    }

    @Test fun `refresh requires body`() = runTest {
        webTestClient.post()
            .uri("/user/refresh")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `refresh requires token`() = runTest {
        val deviceInfo = DeviceInfoRequestDto("device")
        webTestClient.post()
            .uri("/user/me")
            .bodyValue(deviceInfo)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires valid token`() = runTest {
        val deviceInfo = DeviceInfoRequestDto("device")
        webTestClient.post()
            .uri("/user/me")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, "Refresh")
            .bodyValue(deviceInfo)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires associated token to account`() = runTest {
        val user = registerUser()
        val refreshToken = jwtService.createRefreshToken(user.info.id!!, user.info.devices.first().id)
        webTestClient.post()
            .uri("/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, refreshToken)
            .bodyValue(DeviceInfoRequestDto(user.info.devices.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token is valid once`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequestDto(user.info.devices.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequestDto(user.info.devices.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token requires associated device`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequestDto("another device"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh returns valid tokens`() = runTest {
        val user = registerUser()
        val response = webTestClient.post()
            .uri("/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequestDto(user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val account = response.responseBody
        val accessToken = response.responseCookies[Constants.ACCESS_TOKEN_COOKIE]
            ?.firstOrNull()?.value
        val refreshToken = response.responseCookies[Constants.REFRESH_TOKEN_COOKIE]
            ?.firstOrNull()?.value

        requireNotNull(account) { "No account provided in response" }
        requireNotNull(accessToken) { "No access token provided in response" }
        requireNotNull(refreshToken) { "No refresh token provided in response" }

        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())

        assertEquals(user.info.id, account.id)

        webTestClient.post()
            .uri("/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, refreshToken)
            .bodyValue(DeviceInfoRequestDto(user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
    }

    @Test fun `logout requires body`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/logout")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `logout deletes all cookies and logs out user`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/user/logout")
            .bodyValue(DeviceInfoRequestDto(user.info.devices.first().id))
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
        val refreshToken = cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value

        assertTrue(accessToken.isNullOrBlank())
        assertTrue(refreshToken.isNullOrBlank())

        val account = response.responseBody

        requireNotNull(account) { "No account provided in response" }
    }
    @Test fun `logout requires authentication`() = runTest {
        webTestClient.post()
            .uri("/account/logout")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `delete requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/user/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `delete deletes all cookies and deletes user`() = runTest {
        val user = registerUser()

        val response = webTestClient.delete()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
        val refreshToken = cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value

        assertTrue(accessToken.isNullOrBlank())
        assertTrue(refreshToken.isNullOrBlank())

        assertEquals(0, userService.findAll().count())
    }

    @Test fun `2fa setup succeeds`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)
        val login = LoginRequest(email, password, DeviceInfoRequestDto(deviceId))

        val response = webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponseDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        val loginRes = webTestClient.post()
            .uri("/user/login")
            .bodyValue(login)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(loginRes)

        assertTrue(loginRes.twoFactorRequired)
        assertNull(loginRes.user)

        val userRes = webTestClient.post()
            .uri("/user/2fa/verify?code=$code")
            .cookie(Constants.TWO_FACTOR_ATTRIBUTE, user.info.getIdOrThrowEx())
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userRes)

        assertTrue(userRes.twoFactorAuthEnabled)
        assertEquals(user.info.getIdOrThrowEx(), userRes.id)
    }
    @Test fun `2fa setup requires authentication`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/user/2fa/setup")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
