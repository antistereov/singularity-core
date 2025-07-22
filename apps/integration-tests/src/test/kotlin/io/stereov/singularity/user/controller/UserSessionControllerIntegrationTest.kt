package io.stereov.singularity.user.controller

import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.dto.UserResponse
import io.stereov.singularity.user.dto.request.*
import io.stereov.singularity.user.dto.response.LoginResponse
import io.stereov.singularity.user.service.mail.MailTokenService
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.MultipartBodyBuilder
import java.time.Instant

class UserSessionControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mailTokenService: MailTokenService

    @Test fun `getAccount returns user account`() = runTest {
        val user = registerUser()

        val responseBody = webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.COOKIE, "${Constants.ACCESS_TOKEN_COOKIE}=${user.accessToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(responseBody) { "Response has empty body" }

        assertEquals(user.info.sensitive.email, responseBody.email)
    }
    @Test fun `getAccount needs authentication`() = runTest {
        webTestClient.get()
            .uri("/api/user/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `login logs in user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)
        val loginRequest = LoginRequest(email, password, DeviceInfoRequest(deviceId))

        val response = webTestClient.post()
            .uri("/api/user/login")
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

        val userResponse = webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userResponse)

        assertEquals(user.info.id, userResponse.id)
        assertEquals(user.info.sensitive.email, userResponse.email)

        assertEquals(deviceId, user.info.sensitive.devices.firstOrNull()?.id)
        assertEquals(1, user.info.sensitive.devices.size)

        assertEquals(1, userService.findAll().count())
    }
    @Test fun `login needs body`() = runTest {
        webTestClient.post()
            .uri("/api/user/login")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `login needs correct body`() = runTest {
        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue("Test")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }
    @Test fun `login needs valid credentials`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest(user.info.sensitive.email, "wrong password", user.info.sensitive.devices.first().toRequestDto()))
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest("another@email.com", "wrong password", user.info.sensitive.devices.first().toRequestDto()))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `login from new device saves device`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val newDeviceId = "newDeviceId"

        val user = registerUser(email, password, deviceId)

        val accessToken = webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest(email, password, DeviceInfoRequest(newDeviceId)))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token provided in response" }

        val userInfo = webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userInfo) { "No UserDetails provided in response" }

        val devices = userService.findById(user.info.id).sensitive.devices

        assertEquals(2, devices.size)
        assertTrue(devices.any { it.id == deviceId })
        assertTrue(devices.any { it.id == newDeviceId })
    }
    @Test fun `login with two factor works as expected`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"

        val user = registerUser(email, password, deviceId, true)

        val loginRequest = LoginRequest(email, password, DeviceInfoRequest(deviceId))

        val response = webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = response.responseBody
        requireNotNull(body)

        requireNotNull(user.twoFactorToken)

        assertTrue(body.twoFactorRequired)
        assertNotNull(body.user)
    }

    @Test fun `register registers new user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val deviceInfo = DeviceInfoRequest(id = deviceId)

        val response = webTestClient.post()
            .uri("/api/user/register")
            .bodyValue(RegisterUserRequest(email = email, password = password, name = "Name", device = deviceInfo))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
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
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userDetails) { "No UserDetails provided in response" }

        val devices = userService.findById(userDto.id).sensitive.devices

        assertEquals(userDto.id, userDetails.id)
        assertEquals(1, devices.size)
        assertEquals(deviceId, devices.first().id)

        assertEquals(1, userService.findAll().count())
    }
    @Test fun `register requires valid credentials`() = runTest {
        val deviceInfo = DeviceInfoRequest("device")
        webTestClient.post()
            .uri("/api/user/register")
            .bodyValue(RegisterUserRequest(email = "invalid", password = "password", name = "Name", device = deviceInfo))
            .exchange()
            .expectStatus().isBadRequest

        webTestClient.post()
            .uri("/api/user/register")
            .bodyValue(RegisterUserRequest(email = "", password = "password", name = "Name", device = deviceInfo))
            .exchange()
            .expectStatus().isBadRequest

        webTestClient.post()
            .uri("/api/user/register")
            .bodyValue(RegisterUserRequest(email = "test@email.com", password = "", name = "Name", device = deviceInfo))
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register needs body`() = runTest {
        webTestClient.post()
            .uri("/api/user/login")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `changeEmail works with 2fa`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)


        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.id, user.info.sensitive.devices.first().id))
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val token = mailTokenService.createVerificationToken(user.info.id, newEmail, user.mailVerificationSecret)

        val res = webTestClient.post()
            .uri("/api/user/mail/verify?token=$token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newEmail, res.email)
        userService.findByEmail(newEmail)
    }
    @Test fun `changeEmail works without 2fa`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val token = mailTokenService.createVerificationToken(user.info.id, newEmail, user.mailVerificationSecret)

        val res = webTestClient.post()
            .uri("/api/user/mail/verify?token=$token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newEmail, res.email)
        userService.findByEmail(newEmail)
    }
    @Test fun `changeEmail changes email`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        val res = webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)
        assertEquals(newEmail, res.email)
        val foundUser = userService.findByEmail(newEmail)
        assertEquals(user.info.id, foundUser.id)
    }
    @Test fun `changeEmail requires authentication`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/user/me/email")
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires body`() = runTest {
        val oldEmail = "old@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `changeEmail requires correct password`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)
        gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, "wrong-password"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, "wrong-password"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same user`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)
        val anotherUser = registerUser("ttest@email.com")

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(anotherUser.info.id, user.info.sensitive.devices.first().id))
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same device`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.id, "another-device"))
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires unexpired step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(
                Constants.STEP_UP_TOKEN_COOKIE,
                twoFactorAuthTokenService.createStepUpToken(user.info.id, user.info.sensitive.devices.first().id, Instant.ofEpochSecond(0))
            )
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires valid step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(
                Constants.STEP_UP_TOKEN_COOKIE,
                "wrong-token"
            )
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires non-existing email`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)
        registerUser(newEmail)

        webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test fun `sendVerificationEmail throws disabled exception`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/mail/verify/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }
    @Test fun `sendPasswordReset throws disabled exception`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/mail/reset-password/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test fun `changePassword works with 2fa`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        val res = webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.id, user.info.sensitive.devices.first().id))
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest(email, newPassword, DeviceInfoRequest("device")))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `changePassword works without 2fa`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword)

        val res = webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest(email, newPassword, DeviceInfoRequest("device")))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `changePassword requires authentication`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.put()
            .uri("/api/user/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires body`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val user = registerUser(email, oldPassword)

        webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `changePassword requires correct password`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangePasswordRequest("wrong-password", newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up token for same user`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)
        val anotherUser = registerUser("another@email.com")

        webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(anotherUser.info.id, user.info.sensitive.devices.first().id))
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up token for same device`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.id, "another-device"))
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires unexpired step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.id, user.info.sensitive.devices.first().id, Instant.ofEpochSecond(0)))
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires valid step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/user/me/password")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, "wrong-token")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `changeUser works`() = runTest {
        val user = registerUser()
        val newName = "MyName"
        val accessToken = user.accessToken

        val res = webTestClient.put()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .bodyValue(ChangeUserRequest(newName))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newName, res.name)
        assertEquals(newName, userService.findById(user.info.id).sensitive.name)
    }
    @Test fun `changeUser requires authentication`() = runTest {
        registerUser()
        val newName = "MyName"

        webTestClient.put()
            .uri("/api/user/me")
            .bodyValue(ChangeUserRequest(newName))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeUser requires body`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken

        webTestClient.put()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `setAvatar works`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken

        val res = webTestClient.put()
            .uri("/api/user/me/avatar")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part("file", ClassPathResource("files/test-image.jpg"))
                }.build()
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody
    }

    @Test fun `checkAuthentication requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/user/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `checkAuthentication returns user`() = runTest {
        val user = registerUser()

        val response = webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response) { "Response body is empty" }

        assertEquals(user.info.id, response.id)
    }

    @Test fun `refresh requires body`() = runTest {
        webTestClient.post()
            .uri("/api/user/refresh")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `refresh requires token`() = runTest {
        val deviceInfo = DeviceInfoRequest("device")
        webTestClient.post()
            .uri("/api/user/refresh")
            .bodyValue(deviceInfo)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires valid token`() = runTest {
        val deviceInfo = DeviceInfoRequest("device")
        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, "Refresh")
            .bodyValue(deviceInfo)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires associated token to account`() = runTest {
        val user = registerUser()
        val refreshToken = userTokenService.createRefreshToken(user.info.id, user.info.sensitive.devices.first().id, Random.generateCode(20))
        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token is valid once`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token requires associated device`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequest("another device"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh returns valid tokens`() = runTest {
        val user = registerUser()
        val response = webTestClient.post()
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, user.refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
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
            .uri("/api/user/refresh")
            .cookie(Constants.REFRESH_TOKEN_COOKIE, refreshToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .exchange()
            .expectStatus().isOk
    }

    @Test fun `logout requires body`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/logout")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `logout deletes all cookies and logs out user`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/api/user/logout")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
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

        assertTrue(userService.findById(user.info.id).sensitive.devices.isEmpty())
    }
    @Test fun `logout requires authentication`() = runTest {
        webTestClient.post()
            .uri("/api/user/logout")
            .bodyValue(DeviceInfoRequest("device"))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `logoutAllDevices works`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId1 = "device"
        val deviceId2 = "device2"
        val registeredUser = registerUser(email, password, deviceId1)

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest(email, password, DeviceInfoRequest(deviceId1)))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest(email, password, DeviceInfoRequest(deviceId2)))
            .exchange()
            .expectStatus().isOk

        var user = userService.findByEmail(email)

        assertEquals(2, user.sensitive.devices.size)
        assertTrue(user.sensitive.devices.any { it.id == deviceId1 })
        assertTrue(user.sensitive.devices.any { it.id == deviceId2 })

        val response = webTestClient.post()
            .uri("/api/user/logout-all")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, registeredUser.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
        val refreshToken = cookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value

        assertTrue(accessToken.isNullOrBlank())
        assertTrue(refreshToken.isNullOrBlank())

        user = userService.findByEmail(email)

        assertTrue(user.sensitive.devices.isEmpty())
    }
    @Test fun `logoutAllDevices requires authentication`() = runTest {
        webTestClient.post()
            .uri("/api/user/logout-all")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `delete requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/user/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `delete deletes all cookies and deletes user`() = runTest {
        val user = registerUser()

        val response = webTestClient.delete()
            .uri("/api/user/me")
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
}
