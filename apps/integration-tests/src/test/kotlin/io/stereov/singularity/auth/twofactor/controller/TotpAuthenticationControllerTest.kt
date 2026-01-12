package io.stereov.singularity.auth.twofactor.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.token.exception.TwoFactorAuthenticationTokenExtractionException
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.dto.request.TotpRecoveryRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorRecoveryResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.principal.core.dto.response.PrincipalResponse
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.util.*

class TotpAuthenticationControllerTest : BaseIntegrationTest() {

    @Test fun `totp setup succeeds`() = runTest {
        val user = registerUser()
        val login = LoginRequest(user.email!!, user.password!!)

        val stepUpToken = stepUpTokenService.create(user.id, user.sessionId).getOrThrow()

        val response = webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpToken.value)
            .exchange()
            .expectStatus().isOk
            .expectBody<TwoFactorSetupResponse>()
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
            .expectBody<PrincipalResponse>()
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.id).getOrThrow()

        requireNotNull(setupRes)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(response.secret, userWith2fa.sensitive.security.twoFactor.totp.secret!!)
        response.recoveryCodes.forEach { responseCode ->
            assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.any { hashService.checkBcrypt(responseCode, it).getOrThrow() })
        }
        val loginRes = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(login)
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginResponse>()
            .returnResult()

        val loginResBody = loginRes.responseBody
        requireNotNull(loginResBody)

        val twoFactorToken = loginRes.extractTwoFactorAuthenticationToken()

        assertTrue(loginResBody.twoFactorRequired)
        assertEquals(user.id, loginResBody.user.id)

        val userRes = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .bodyValue(CompleteStepUpRequest(totp = code))
            .exchange()
            .expectStatus().isOk
            .expectBody<LoginResponse>()
            .returnResult()

        val body = requireNotNull(userRes.responseBody)

        assertTrue(body.user.twoFactorAuthEnabled)
        assertEquals(user.id, body.user.id)
        val accessToken = userRes.extractAccessToken()
        val refreshToken = userRes.extractRefreshToken()

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
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

        val stepUpToken = stepUpTokenService.create(user.id, user.sessionId).getOrThrow()

        val response = webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpToken).getOrThrow().value)
            .exchange()
            .expectStatus().isOk
            .expectBody<TwoFactorSetupResponse>()
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
            .expectBody<PrincipalResponse>()
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.id).getOrThrow()

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

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        val setupRes = webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isOk
            .expectBody<PrincipalResponse>()
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.id).getOrThrow()

        requireNotNull(setupRes)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(secret, userWith2fa.sensitive.security.twoFactor.totp.secret!!)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.any { hashService.checkBcrypt(recoveryCode, it).getOrThrow() })

    }
    @Test fun `totp setup validation needs valid token`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs correct code`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs unexpired token`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode), Instant.ofEpochSecond(0)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs token for same user`() = runTest {
        val user = registerUser()
        val anotherUser = registerUser()

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(anotherUser.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs authentication`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs step-up`() = runTest {
        val user = registerUser()

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation requires password authentication`() = runTest {
        val user = registerOAuth2()

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation is not modified when enabled already`() = runTest {
        val user = registerUser(totpEnabled = true)

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.id, secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isNotModified

        val userWith2fa = userService.findById(user.id).getOrThrow()

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
            .expectBody<TwoFactorRecoveryResponse>()
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.id, body.user.id)
        assertThrows<TwoFactorAuthenticationTokenExtractionException> { res.extractTwoFactorAuthenticationToken() }

        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        val stepUpToken = res.extractStepUpToken(user.id, accessToken.sessionId)

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(user.id, stepUpToken.userId)

        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(refreshToken.sessionId, stepUpToken.sessionId)

        val userAfterRecovery = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.totp.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.contains(hashService.hashBcrypt(user.totpRecovery).getOrThrow()))
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
            .expectBody<TwoFactorRecoveryResponse>()
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertThrows<TwoFactorAuthenticationTokenExtractionException> { res.extractTwoFactorAuthenticationToken() }

        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        val stepUpToken = res.extractStepUpToken(user.id, accessToken.sessionId)

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(user.id, stepUpToken.userId)

        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(refreshToken.sessionId, stepUpToken.sessionId)

        val userAfterRecovery = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.totp.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.contains(hashService.hashBcrypt(user.totpRecovery).getOrThrow()))
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
            .twoFactorAuthenticationTokenCookie(twoFactorAuthenticationTokenService.create(user.id).getOrThrow().value)
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
            .expectBody<LoginResponse>()
            .returnResult()
            .extractAccessToken()

        val updatedUser = userService.findById(user.id).getOrThrow()

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
            .expectBody<PrincipalResponse>()
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.id, body.id)
        assertTrue(body.twoFactorAuthEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), body.twoFactorMethods)

        val userAfterDisable = userService.findById(user.id).getOrThrow()
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

        val userAfterDisable = userService.findById(user.id).getOrThrow()
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

        val userAfterDisable = userService.findById(user.id).getOrThrow()
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

        val userAfterDisable = userService.findById(user.id).getOrThrow()
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

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires unexpired step up token`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpTokenService.create(user.id, user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()).getOrThrow().value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires step up token for same user`() = runTest {
        val user = registerUser(totpEnabled = true)

        val anotherUser = registerUser("another@email.com")

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpTokenService.create(anotherUser.id, user.sessionId).getOrThrow()).getOrThrow().value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires step up token for same session`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpTokenService.create(user.id,
                UUID.randomUUID()).getOrThrow()).getOrThrow().value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
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
            .expectStatus().isNotModified
    }
}
