package io.stereov.singularity.user.controller

import io.stereov.singularity.auth.device.dto.DeviceInfoRequest
import io.stereov.singularity.auth.session.dto.request.LoginRequest
import io.stereov.singularity.auth.session.dto.response.LoginResponse
import io.stereov.singularity.auth.session.model.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.request.DisableTwoFactorRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorSetupInitRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.StepUpResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorRecoveryResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorStatusResponse
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TwoFactorSetupTokenService
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class UserTwoFactorAuthControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var setupTokenService: TwoFactorSetupTokenService

    @Test fun `2fa setup succeeds`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)
        val login = LoginRequest(email, password, DeviceInfoRequest(deviceId))

        val twoFactorSetupStartRes = webTestClient.post()
            .uri("/api/user/2fa/init-setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorSetupInitRequest(password))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseCookies[TwoFactorTokenType.InitSetup.cookieKey]?.firstOrNull()?.value

        requireNotNull(twoFactorSetupStartRes)

        val response = webTestClient.get()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.InitSetup.cookieKey, twoFactorSetupStartRes)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        val setupRes = webTestClient.post()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(response.token, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.id)

        requireNotNull(setupRes)
        assertTrue(userWith2fa.sensitive.security.twoFactor.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(response.secret, userWith2fa.sensitive.security.twoFactor.secret!!)
        response.recoveryCodes.forEach { responseCode ->
            assertTrue(userWith2fa.sensitive.security.twoFactor.recoveryCodes.any { hashService.checkBcrypt(responseCode, it) })
        }
        val loginRes = webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(login)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val loginResBody = loginRes.responseBody
        requireNotNull(loginResBody)

        val twoFactorToken = loginRes.responseCookies[TwoFactorTokenType.Login.cookieKey]
            ?.first()
            ?.value

        requireNotNull(twoFactorToken)

        assertTrue(loginResBody.twoFactorRequired)
        assertEquals(user.info.id, loginResBody.user.id)

        val userRes = webTestClient.post()
            .uri("/api/user/2fa/verify-login?code=$code")
            .cookie(TwoFactorTokenType.Login.cookieKey, twoFactorToken)
            .bodyValue(DeviceInfoRequest(deviceId))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userRes)

        assertTrue(userRes.user.twoFactorAuthEnabled)
        assertEquals(user.info.id, userRes.user.id)
    }
    @Test fun `2fa setup requires authentication`() = runTest {
        val password = "password"
        val user = registerUser()

        val twoFactorSetupStartRes = webTestClient.post()
            .uri("/api/user/2fa/init-setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorSetupInitRequest(password))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseCookies[TwoFactorTokenType.InitSetup.cookieKey]?.firstOrNull()?.value

        requireNotNull(twoFactorSetupStartRes)

        webTestClient.get()
            .uri("/api/user/2fa/setup")
            .cookie(TwoFactorTokenType.InitSetup.cookieKey, twoFactorSetupStartRes)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa setup requires setup token`() = runTest {
        val user = registerUser()
        webTestClient.get()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken).exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa setup clears all devices`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val twoFactorSetupStartRes = webTestClient.post()
            .uri("/api/user/2fa/init-setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorSetupInitRequest(password))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
            .responseCookies[TwoFactorTokenType.InitSetup.cookieKey]?.firstOrNull()?.value

        requireNotNull(twoFactorSetupStartRes)

        val response = webTestClient.get()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.InitSetup.cookieKey, twoFactorSetupStartRes)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        webTestClient.post()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(response.token, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.id)

        assertTrue(userWith2fa.sensitive.devices.isEmpty())
    }

    @Test fun `2fa setup validation works`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = Random.generateCode(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        val setupRes = webTestClient.post()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.id)

        requireNotNull(setupRes)
        assertTrue(userWith2fa.sensitive.security.twoFactor.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(secret, userWith2fa.sensitive.security.twoFactor.secret!!)
        assertTrue(userWith2fa.sensitive.security.twoFactor.recoveryCodes.any { hashService.checkBcrypt(recoveryCode, it) })

    }
    @Test fun `2fa setup validation needs valid token`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = Random.generateCode(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.recoveryCodes.isEmpty())
    }
    @Test fun `2fa setup validation needs unexpired token`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = Random.generateCode(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode), Instant.ofEpochSecond(0))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.recoveryCodes.isEmpty())
    }
    @Test fun `2fa setup validation needs token for same user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)
        val anotherUser = registerUser("another@email.com")

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = Random.generateCode(10)
        val token = setupTokenService.create(anotherUser.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/user/2fa/setup")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.recoveryCodes.isEmpty())
    }
    @Test fun `2fa setup validation needs authentication`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)

        val secret = twoFactorAuthService.generateSecretKey()
        val recoveryCode = Random.generateCode(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/user/2fa/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.recoveryCodes.isEmpty())
    }

    @Test fun `2fa recovery works with login verification token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.twoFactorRecovery)

        val recoveryCodeCount = user.info.sensitive.security.twoFactor.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/api/user/2fa/recovery?code=${user.twoFactorRecovery}")
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorRecoveryResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Login.cookieKey]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[SessionTokenType.Access.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[TwoFactorTokenType.StepUp.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())

        val userAfterRecovery = userService.findById(user.info.id)
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.recoveryCodes.contains(hashService.hashBcrypt(user.twoFactorRecovery)))
    }
    @Test fun `2fa recovery works with access token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.twoFactorRecovery)

        val recoveryCodeCount = user.info.sensitive.security.twoFactor.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/api/user/2fa/recovery?code=${user.twoFactorRecovery}")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorRecoveryResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Login.cookieKey]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[SessionTokenType.Access.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[TwoFactorTokenType.StepUp.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())

        val userAfterRecovery = userService.findById(user.info.id)
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.recoveryCodes.contains(hashService.hashBcrypt(user.twoFactorRecovery)))
    }
    @Test fun `2fa recovery needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.twoFactorSecret) + 1

        webTestClient.post()
            .uri("/api/user/2fa/recovery?code=$code?context=login")
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa recovery needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/user/2fa/recovery")
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `2fa recovery needs login verification or access token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/user/2fa/recovery?code=25234")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa recovery needs body`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/user/2fa/recovery?code=25234")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `verifyLogin works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.twoFactorSecret)

        val res = webTestClient.post()
            .uri("/api/user/2fa/verify-login?code=$code")
            .bodyValue(user.info.sensitive.devices.first().toRequestDto())
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Login.cookieKey]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[SessionTokenType.Access.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieKey]?.firstOrNull()?.value.isNullOrEmpty())
    }
    @Test fun `verifyLogin needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.twoFactorSecret) + 1

        webTestClient.post()
            .uri("/api/user/2fa/verify-login?code=$code")
            .bodyValue(user.info.sensitive.devices.first().toRequestDto())
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/user/2fa/verify-login")
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
            .bodyValue(user.info.sensitive.devices.first())
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin needs 2fa token`() = runTest {
        webTestClient.post()
            .uri("/api/user/2fa/verify-login?code=25234")
            .bodyValue(DeviceInfoRequest("device"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin needs body`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/user/2fa/verify-login")
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `loginStatus works is not pending if no cookie is set`() = runTest {
        val status0 = webTestClient.get()
            .uri("/api/user/2fa/login-status")
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        assertFalse(status0?.twoFactorRequired!!)
    }
    @Test fun `loginStatus not pending with invalid cookie`() = runTest {
        val status1 = webTestClient.get()
            .uri("/api/user/2fa/login-status")
            .cookie(TwoFactorTokenType.Login.cookieKey, "test")
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
            .uri("/api/user/2fa/login-status")
            .cookie(TwoFactorTokenType.Login.cookieKey, user.twoFactorToken)
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
            .uri("/api/user/2fa/verify-step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpResponse::class.java)
            .returnResult()

        val token = res.responseCookies[TwoFactorTokenType.StepUp.cookieKey]?.first()?.value

        requireNotNull(token)
    }
    @Test fun `verifyStepUp requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.post()
            .uri("/api/user/2fa/verify-step-up?code=$twoFactorCode")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp requires 2fa code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/api/user/2fa/verify-step-up?code")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp requires valid 2fa code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.twoFactorSecret) + 1

        webTestClient.post()
            .uri("/api/user/2fa/verify-step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp requires enabled 2fa`() = runTest {
        val user = registerUser()
        val twoFactorCode = gAuth.getTotpPassword(user.twoFactorSecret) + 1

        webTestClient.post()
            .uri("/api/user/2fa/verify-step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `stepUpStatus works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id)).value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse ::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertFalse(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires token for same user`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val anotherUser = registerUser("another@email.com")

        val res = webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(anotherUser.info.id, user.info.sensitive.devices.first().id)).value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires token for same device`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, "another-device")).value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires valid token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, "another-token")
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires unexpired token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id, Instant.ofEpochSecond(0))).value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id)).value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUpStatus 2fa is not required for disabled 2fa when cookie is set`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id)).value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertFalse(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus is not required for disabled 2fa when cookie is not set`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/user/2fa/step-up-status")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertFalse(res.twoFactorRequired)
    }

    @Test fun `disable works`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)

        val stepUp = stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id)
        val req = DisableTwoFactorRequest(password)

        val res = webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUp).value)
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.id)
        assertFalse(body.twoFactorAuthEnabled)

        val userAfterDisable = userService.findById(user.info.id)
        assertFalse(userAfterDisable.sensitive.security.twoFactor.enabled)
    }
    @Test fun `disable requires authentication`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)
        val req = DisableTwoFactorRequest(password)
        val token = stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id)

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(token).value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)
        val req = DisableTwoFactorRequest(password)

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires valid step up token`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)
        val req = DisableTwoFactorRequest(password)

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, "test")
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires unexpired step up token`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)
        val req = DisableTwoFactorRequest(password)

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id, Instant.ofEpochSecond(0))).value)
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token for same user`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)
        val req = DisableTwoFactorRequest(password)

        val anotherUser = registerUser("another@email.com")

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(anotherUser.info.id, user.info.sensitive.devices.first().id)).value)
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token for same device`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)
        val req = DisableTwoFactorRequest(password)

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, "another-device")).value)
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires 2fa to be enabled`() = runTest {
        val password = "password"
        val user = registerUser(password = password)

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(DisableTwoFactorRequest(password))
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id)).value)
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `disable needs correct password`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val stepUp = stepUpTokenService.create(user.info.id, user.info.sensitive.devices.first().id)
        val req = DisableTwoFactorRequest("wrong-password")

        webTestClient.post()
            .uri("/api/user/2fa/disable")
            .bodyValue(req)
            .cookie(TwoFactorTokenType.StepUp.cookieKey, cookieCreator.createCookie(stepUp).value)
            .cookie(SessionTokenType.Access.cookieKey, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
