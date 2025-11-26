package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.dto.request.TotpRecoveryRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorRecoveryResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.principal.core.dto.response.UserResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.*

class TotpAuthenticationControllerTest : BaseIntegrationTest() {

    @Test fun `totp setup succeeds`() = runTest {
        val user = registerUser()
        val login = LoginRequest(user.email!!, user.password!!)

        val stepUpToken = stepUpTokenService.create(user.info.id, user.sessionId)

        val response = webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpToken.value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        val setupRes = webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpToken.value)
            .bodyValue(TwoFactorVerifySetupRequest(response.token, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.id)

        requireNotNull(setupRes)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(response.secret, userWith2fa.sensitive.security.twoFactor.totp.secret!!)
        response.recoveryCodes.forEach { responseCode ->
            assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.any { hashService.checkBcrypt(responseCode, it) })
        }
        val loginRes = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(login)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val loginResBody = loginRes.responseBody
        requireNotNull(loginResBody)

        val twoFactorToken = loginRes.extractTwoFactorAuthenticationToken()

        assertTrue(loginResBody.twoFactorRequired)
        assertEquals(user.info.id, loginResBody.user.id)

        val userRes = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .bodyValue(CompleteStepUpRequest(totp = code))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = requireNotNull(userRes.responseBody)

        assertTrue(body.user.twoFactorAuthEnabled)
        assertEquals(user.info.id, body.user.id)
        val accessToken = userRes.extractAccessToken()
        val refreshToken = userRes.extractRefreshToken()

        assertEquals(user.info.id, accessToken.userId)
        assertEquals(user.info.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
    }

    @Test fun `totp get setup details requires authentication`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp get setup details requires step-up token`() = runTest {
        val user = registerUser()
        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp get setup details clears all sessions`() = runTest {
        val email = "test@email.com"
        val user = registerUser(email)

        val stepUpToken = stepUpTokenService.create(user.info.id, user.sessionId)

        val response = webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpToken).value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(response.token, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.id)

        assertTrue(userWith2fa.sensitive.sessions.isEmpty())
    }
    @Test fun `totp get setup details requires totp to be disabled`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isNotModified
    }
    @Test fun `totp get setup details requires password authentication`() = runTest {
        val user = registerOAuth2()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `totp get setup details is not modified when enabled already`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isNotModified
    }

    @Test fun `totp setup validation works`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        val setupRes = webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.info.id)

        requireNotNull(setupRes)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(secret, userWith2fa.sensitive.security.twoFactor.totp.secret!!)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.any { hashService.checkBcrypt(recoveryCode, it) })

    }
    @Test fun `totp setup validation needs valid token`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs correct code`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs unexpired token`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode), Instant.ofEpochSecond(0))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs token for same user`() = runTest {
        val user = registerUser()
        val anotherUser = registerUser()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(anotherUser.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs authentication`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs step-up`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation requires password authentication`() = runTest {
        val user = registerOAuth2()

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation is not modified when enabled already`() = runTest {
        val user = registerUser(totpEnabled = true)

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isNotModified

        val userWith2fa = userService.findById(user.info.id)

        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertEquals(user.totpSecret, userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }

    @Test fun `totp recovery works with login verification token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.totpRecovery)

        val recoveryCodeCount = user.info.sensitive.security.twoFactor.totp.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/api/auth/2fa/totp/recover")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .bodyValue(TotpRecoveryRequest(user.totpRecovery, null))
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorRecoveryResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.user.id)
        assertThrows<TokenExtractionException> { res.extractTwoFactorAuthenticationToken() }

        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        val stepUpToken = res.extractStepUpToken(user.info.id, accessToken.sessionId)

        assertEquals(user.info.id, accessToken.userId)
        assertEquals(user.info.id, refreshToken.userId)
        assertEquals(user.info.id, stepUpToken.userId)

        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(refreshToken.sessionId, stepUpToken.sessionId)

        val userAfterRecovery = userService.findById(user.info.id)
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.totp.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.contains(hashService.hashBcrypt(user.totpRecovery)))
    }
    @Test fun `totp recovery works authorized`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.totpRecovery)

        val recoveryCodeCount = user.info.sensitive.security.twoFactor.totp.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/api/auth/2fa/totp/recover")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .bodyValue(TotpRecoveryRequest(user.totpRecovery, null))
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorRecoveryResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertThrows<TokenExtractionException> { res.extractTwoFactorAuthenticationToken() }

        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        val stepUpToken = res.extractStepUpToken(user.info.id, accessToken.sessionId)

        assertEquals(user.info.id, accessToken.userId)
        assertEquals(user.info.id, refreshToken.userId)
        assertEquals(user.info.id, stepUpToken.userId)

        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(refreshToken.sessionId, stepUpToken.sessionId)

        val userAfterRecovery = userService.findById(user.info.id)
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.totp.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.contains(hashService.hashBcrypt(user.totpRecovery)))
    }
    @Test fun `totp recovery needs correct code`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/totp/recover")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .bodyValue(TotpRecoveryRequest(code.toString(), null))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp recovery needs 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/recover")
            .accessTokenCookie(user.accessToken)
            .bodyValue(TotpRecoveryRequest(user.totpRecovery!!, null))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp recovery needs 2fa enabled `() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/2fa/totp/recover")
            .twoFactorAuthenticationTokenCookie(twoFactorAuthenticationTokenService.create(user.info.id).value)
            .bodyValue(TotpRecoveryRequest("random-code", null))
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `totp recovery needs body`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/recover")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `totp recovery saves session data correctly`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val req = TotpRecoveryRequest(
            code = user.totpRecovery!!,
            session = SessionInfoRequest("browser", "os")
        )

        val accessToken = webTestClient.post()
            .uri("/api/auth/2fa/totp/recover")
            .bodyValue(req)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .extractAccessToken()

        val updatedUser = userService.findById(user.info.id)

        assertEquals(2, updatedUser.sensitive.sessions.size)
        assertEquals(req.session!!.browser, updatedUser.sensitive.sessions[accessToken.sessionId]!!.browser)
        assertEquals(req.session!!.os, updatedUser.sensitive.sessions[accessToken.sessionId]!!.os)
    }

    @Test fun `disable works`() = runTest {
        val user = registerUser(totpEnabled = true)
        user.info.sensitive.security.twoFactor.email.enabled = true
        userService.save(user.info)

        val res = webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(user.stepUpToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.id)
        assertTrue(body.twoFactorAuthEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), body.twoFactorMethods)

        val userAfterDisable = userService.findById(user.info.id)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertFalse(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires another 2fa method to be enabled`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(user.stepUpToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        val userAfterDisable = userService.findById(user.info.id)
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires authentication`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.info.id)
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires step up token`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.info.id)
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires valid step up token`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie("test")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.info.id)
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires unexpired step up token`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.sessionId, Instant.ofEpochSecond(0))).value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.info.id)
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires step up token for same user`() = runTest {
        val user = registerUser(totpEnabled = true)

        val anotherUser = registerUser("another@email.com")

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpTokenService.create(anotherUser.info.id, user.sessionId)).value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.info.id)
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires step up token for same session`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpTokenService.create(user.info.id,
                UUID.randomUUID())).value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.info.id)
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires 2fa to be enabled`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(user.stepUpToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
}
