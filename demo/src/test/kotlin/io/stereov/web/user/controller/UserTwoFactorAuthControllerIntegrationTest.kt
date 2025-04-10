package io.stereov.web.user.controller

import io.stereov.web.config.Constants
import io.stereov.web.global.service.random.RandomService
import io.stereov.web.test.BaseIntegrationTest
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.dto.request.DeviceInfoRequest
import io.stereov.web.user.dto.request.LoginRequest
import io.stereov.web.user.dto.request.TwoFactorSetupRequest
import io.stereov.web.user.dto.response.LoginResponse
import io.stereov.web.user.dto.response.StepUpStatusResponse
import io.stereov.web.user.dto.response.TwoFactorSetupResponse
import io.stereov.web.user.dto.response.TwoFactorStatusResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class UserTwoFactorAuthControllerIntegrationTest : BaseIntegrationTest() {

    @Test fun `2fa setup succeeds`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)
        val login = LoginRequest(email, password, DeviceInfoRequest(deviceId))

        val response = webTestClient.get()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        val setupRes = webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(TwoFactorSetupRequest(response.token, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.idX)

        requireNotNull(setupRes)
        assertTrue(userWith2fa.security.twoFactor.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(response.secret, encryptionService.decrypt(userWith2fa.security.twoFactor.secret!!))
        response.recoveryCodes.forEach { responseCode ->
            assertTrue(userWith2fa.security.twoFactor.recoveryCodes.any { hashService.checkBcrypt(responseCode, it) })
        }
        val loginRes = webTestClient.post()
            .uri("/user/login")
            .bodyValue(login)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val loginResBody = loginRes.responseBody
        requireNotNull(loginResBody)

        val twoFactorToken = loginRes.responseCookies[Constants.LOGIN_VERIFICATION_TOKEN]
            ?.first()
            ?.value

        requireNotNull(twoFactorToken)

        assertTrue(loginResBody.twoFactorRequired)
        assertEquals(user.info.id, loginResBody.user.id)

        val userRes = webTestClient.post()
            .uri("/user/2fa/verify-login?code=$code")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, twoFactorToken)
            .bodyValue(DeviceInfoRequest(deviceId))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userRes)

        assertTrue(userRes.twoFactorAuthEnabled)
        assertEquals(user.info.idX, userRes.id)
    }
    @Test fun `2fa setup requires authentication`() = runTest {
        registerUser()
        webTestClient.get()
            .uri("/user/2fa/setup")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa setup clears all devices`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val response = webTestClient.get()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(TwoFactorSetupRequest(response.token, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.idX)

        assertTrue(userWith2fa.devices.isEmpty())
    }

    @Test fun `2fa setup validation works`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = RandomService.generateCode(10)
        val token = twoFactorAuthTokenService.createSetupToken(user.info.idX, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        val setupRes = webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(TwoFactorSetupRequest(token, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.idX)

        requireNotNull(setupRes)
        assertTrue(userWith2fa.security.twoFactor.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(secret, encryptionService.decrypt(userWith2fa.security.twoFactor.secret!!))
        assertTrue(userWith2fa.security.twoFactor.recoveryCodes.any { hashService.checkBcrypt(recoveryCode, it) })

    }
    @Test fun `2fa setup validation needs valid token`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = RandomService.generateCode(10)
        val token = twoFactorAuthTokenService.createSetupToken(user.info.idX, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(TwoFactorSetupRequest(token + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.idX)

        assertFalse(userWith2fa.security.twoFactor.enabled)
        assertNull(userWith2fa.security.twoFactor.secret)
        assertTrue(userWith2fa.security.twoFactor.recoveryCodes.isEmpty())
    }
    @Test fun `2fa setup validation needs unexpired token`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = RandomService.generateCode(10)
        val token = twoFactorAuthTokenService.createSetupToken(user.info.idX, secret, listOf(recoveryCode), Instant.ofEpochSecond(0))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(TwoFactorSetupRequest(token, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.idX)

        assertFalse(userWith2fa.security.twoFactor.enabled)
        assertNull(userWith2fa.security.twoFactor.secret)
        assertTrue(userWith2fa.security.twoFactor.recoveryCodes.isEmpty())
    }
    @Test fun `2fa setup validation needs token for same user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = RandomService.generateCode(10)
        val token = twoFactorAuthTokenService.createSetupToken("another user", secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(TwoFactorSetupRequest(token, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.idX)

        assertFalse(userWith2fa.security.twoFactor.enabled)
        assertNull(userWith2fa.security.twoFactor.secret)
        assertTrue(userWith2fa.security.twoFactor.recoveryCodes.isEmpty())
    }
    @Test fun `2fa setup validation needs authentication`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = RandomService.generateCode(10)
        val token = twoFactorAuthTokenService.createSetupToken(user.info.idX, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/user/2fa/setup")
            .bodyValue(TwoFactorSetupRequest(token + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.idX)

        assertFalse(userWith2fa.security.twoFactor.enabled)
        assertNull(userWith2fa.security.twoFactor.secret)
        assertTrue(userWith2fa.security.twoFactor.recoveryCodes.isEmpty())
    }

    @Test fun `2fa recovery works with login verification token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.twoFactorRecovery)

        val recoveryCodeCount = user.info.security.twoFactor.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/user/2fa/recovery?code=${user.twoFactorRecovery}")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .bodyValue(DeviceInfoRequest(user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.id)
        val token = res.responseCookies[Constants.LOGIN_VERIFICATION_TOKEN]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value.isNullOrEmpty())

        val userAfterRecovery = userService.findById(user.info.idX)
        assertTrue(userAfterRecovery.security.twoFactor.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.security.twoFactor.recoveryCodes.size)
        assertFalse(userAfterRecovery.security.twoFactor.recoveryCodes.contains(user.twoFactorRecovery))
    }
    @Test fun `2fa recovery works with access token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.twoFactorRecovery)

        val recoveryCodeCount = user.info.security.twoFactor.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/user/2fa/recovery?code=${user.twoFactorRecovery}")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.id)
        val token = res.responseCookies[Constants.LOGIN_VERIFICATION_TOKEN]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value.isNullOrEmpty())

        val userAfterRecovery = userService.findById(user.info.idX)
        assertTrue(userAfterRecovery.security.twoFactor.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.security.twoFactor.recoveryCodes.size)
        assertFalse(userAfterRecovery.security.twoFactor.recoveryCodes.contains(user.twoFactorRecovery))
    }
    @Test fun `2fa recovery needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.info.security.twoFactor.secret) + 1

        webTestClient.post()
            .uri("/user/2fa/recovery?code=$code")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .bodyValue(DeviceInfoRequest(user.info.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa recovery needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/user/2fa/recovery")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .bodyValue(DeviceInfoRequest(user.info.devices.first().id))
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `2fa recovery needs login verification or access token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/user/2fa/recovery?code=25234")
            .bodyValue(DeviceInfoRequest(user.info.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa recovery needs body`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/user/2fa/recovery?code=25234")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `verifyLogin works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.twoFactorSecret)

        val res = webTestClient.post()
            .uri("/user/2fa/verify-login?code=$code")
            .bodyValue(user.info.devices.first().toRequestDto())
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.toDto(), body)
        val token = res.responseCookies[Constants.LOGIN_VERIFICATION_TOKEN]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value.isNullOrEmpty())
    }
    @Test fun `verifyLogin needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.info.security.twoFactor.secret) + 1

        webTestClient.post()
            .uri("/user/2fa/verify-login?code=$code")
            .bodyValue(user.info.devices.first().toRequestDto())
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/user/2fa/verify-login")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .bodyValue(user.info.devices.first())
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin needs 2fa token`() = runTest {
        webTestClient.post()
            .uri("/user/2fa/verify-login?code=25234")
            .bodyValue(DeviceInfoRequest("device"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin needs body`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/user/2fa/verify-login")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `loginStatus works is not pending if no cookie is set`() = runTest {
        val status0 = webTestClient.get()
            .uri("/user/2fa/login-status")
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        assertFalse(status0?.twoFactorRequired!!)
    }
    @Test fun `loginStatus not pending with invalid cookie`() = runTest {
        val status1 = webTestClient.get()
            .uri("/user/2fa/login-status")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, "test")
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        assertFalse(status1?.twoFactorRequired!!)
    }
    @Test fun `loginStatus works when valid cookie is set`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        requireNotNull(user.twoFactorToken)

        val res = webTestClient.get()
            .uri("/user/2fa/login-status")
            .cookie(Constants.LOGIN_VERIFICATION_TOKEN, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        assertTrue(res?.twoFactorRequired!!)
    }

    @Test fun `verifyStepUp works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.twoFactorSecret)

        val res = webTestClient.post()
            .uri("/user/2fa/verify-step-up?code=$twoFactorCode")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()

        val token = res.responseCookies[Constants.STEP_UP_TOKEN_COOKIE]?.first()?.value

        requireNotNull(token)
    }
    @Test fun `verifyStepUp requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.post()
            .uri("/user/2fa/verify-step-up?code=$twoFactorCode")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp requires 2fa code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/user/2fa/verify-step-up?code")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp requires valid 2fa code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.twoFactorSecret) + 1

        webTestClient.post()
            .uri("/user/2fa/verify-step-up?code=$twoFactorCode")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp requires enabled 2fa`() = runTest {
        val user = registerUser()
        val twoFactorCode = gAuth.getTotpPassword(user.twoFactorSecret) + 1

        webTestClient.post()
            .uri("/user/2fa/verify-step-up?code=$twoFactorCode")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `stepUpStatus works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.stepUp)
    }
    @Test fun `stepUpStatus requires token for same user`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken("another-user", user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertFalse(res.stepUp)
    }
    @Test fun `stepUpStatus requires token for same device`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, "another-device"))
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertFalse(res.stepUp)
    }
    @Test fun `stepUpStatus requires valid token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, "another-token")
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertFalse(res.stepUp)
    }
    @Test fun `stepUpStatus requires unexpired token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id, Instant.ofEpochSecond(0)))
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertFalse(res.stepUp)
    }
    @Test fun `stepUpStatus requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUpStatus is true for disabled 2fa when cookie is set`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.stepUp)
    }
    @Test fun `stepUpStatus is true for disabled 2fa when cookie is not set`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/user/2fa/step-up-status")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.stepUp)
    }

    @Test fun `disable works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val stepUp = twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id)

        val res = webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, stepUp)
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.id)
        assertFalse(body.twoFactorAuthEnabled)

        val userAfterDisable = userService.findById(user.info.idX)
        assertFalse(userAfterDisable.security.twoFactor.enabled)
    }
    @Test fun `disable requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires valid step up token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, "test")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires unexpired step up token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id, Instant.ofEpochSecond(0)))
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token for same user`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken("another-user", user.info.devices.first().id))
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token for same device`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, "another-device"))
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires 2fa to be enabled`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/2fa/disable")
            .cookie(Constants.STEP_UP_TOKEN_COOKIE, twoFactorAuthTokenService.createStepUpToken(user.info.idX, user.info.devices.first().id))
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
}
