package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.request.DisableTwoFactorRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorAuthenticationRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorRecoveryResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.token.TotpSetupTokenService
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.*

class TotpAuthenticationControllerTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var setupTokenService: TotpSetupTokenService

    @Test fun `totp setup succeeds`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val user = registerUser(email, password)
        val login = LoginRequest(email, password)

        val stepUpToken = stepUpTokenService.create(user.info.id, user.sessionId)

        val response = webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, stepUpToken.value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        val setupRes = webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, stepUpToken.value)
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

        val twoFactorToken = loginRes.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.first()
            ?.value

        requireNotNull(twoFactorToken)

        assertTrue(loginResBody.twoFactorRequired)
        assertEquals(user.info.id, loginResBody.user.id)

        val userRes = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .cookie(TwoFactorTokenType.Authentication.cookieName, twoFactorToken)
            .bodyValue(TwoFactorAuthenticationRequest(totp = code))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userRes)

        assertTrue(userRes.user.twoFactorAuthEnabled)
        assertEquals(user.info.id, userRes.user.id)
    }

    @Test fun `totp get setup details requires authentication`() = runTest {
        val user = registerUser()

        val stepUpToken = stepUpTokenService.create(user.info.id, user.sessionId)

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp get setup details requires step-up token`() = runTest {
        val user = registerUser()
        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp get setup details clears all sessions`() = runTest {
        val email = "test@email.com"
        val user = registerUser(email)

        val stepUpToken = stepUpTokenService.create(user.info.id, user.sessionId)

        val response = webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpToken).value)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
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
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test fun `totp setup validation works`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val user = registerUser(email, password, )

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        val setupRes = webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
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
        val email = "test@email.com"
        val password = "password"
        val user = registerUser(email, password)

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs unexpired token`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val user = registerUser(email, password,)

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode), Instant.ofEpochSecond(0))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs token for same user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val user = registerUser(email, password)
        val anotherUser = registerUser("another@email.com")

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(anotherUser.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }
    @Test fun `totp setup validation needs authentication`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val user = registerUser(email, password)

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .bodyValue(TwoFactorVerifySetupRequest(token.value + "a", code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.info.id)

        assertFalse(userWith2fa.sensitive.security.twoFactor.totp.enabled)
        assertNull(userWith2fa.sensitive.security.twoFactor.totp.secret)
        assertTrue(userWith2fa.sensitive.security.twoFactor.totp.recoveryCodes.isEmpty())
    }

    @Test fun `totp recovery works with login verification token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.totpRecovery)

        val recoveryCodeCount = user.info.sensitive.security.twoFactor.totp.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/api/auth/2fa/recovery?code=${user.totpRecovery}")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorRecoveryResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[SessionTokenType.StepUp.cookieName]?.firstOrNull()?.value.isNullOrEmpty())

        val userAfterRecovery = userService.findById(user.info.id)
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.totp.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.contains(hashService.hashBcrypt(user.totpRecovery)))
    }
    @Test fun `totp recovery works with access token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.totpRecovery)

        val recoveryCodeCount = user.info.sensitive.security.twoFactor.totp.recoveryCodes.size

        val res = webTestClient.post()
            .uri("/api/auth/2fa/recovery?code=${user.totpRecovery}")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorRecoveryResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.first()
            ?.value
        assertEquals("", token)

        assertFalse(res.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
        assertFalse(res.responseCookies[SessionTokenType.StepUp.cookieName]?.firstOrNull()?.value.isNullOrEmpty())

        val userAfterRecovery = userService.findById(user.info.id)
        assertTrue(userAfterRecovery.sensitive.security.twoFactor.totp.enabled)
        assertEquals(recoveryCodeCount - 1, userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.size)
        assertFalse(userAfterRecovery.sensitive.security.twoFactor.totp.recoveryCodes.contains(hashService.hashBcrypt(user.totpRecovery)))
    }
    @Test fun `totp recovery needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/recovery?code=$code?context=login")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp recovery needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/recovery")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `totp recovery needs login verification or access token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/recovery?code=25234")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `totp recovery needs body`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/recovery?code=25234")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `disable works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.id)
        assertFalse(body.twoFactorAuthEnabled)

        val userAfterDisable = userService.findById(user.info.id)
        assertFalse(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
    }
    @Test fun `disable requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val token = stepUpTokenService.create(user.info.id, user.sessionId)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires valid step up token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, "test")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires unexpired step up token`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)
        val req = DisableTwoFactorRequest(password)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpTokenService.create(user.info.id, user.sessionId, Instant.ofEpochSecond(0))).value)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token for same user`() = runTest {
        val password = "password"
        val user = registerUser(twoFactorEnabled = true)

        val anotherUser = registerUser("another@email.com")

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpTokenService.create(anotherUser.info.id, user.sessionId)).value)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires step up token for same session`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpTokenService.create(user.info.id,
                UUID.randomUUID())).value)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `disable requires 2fa to be enabled`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `disable needs correct password`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val stepUp = stepUpTokenService.create(user.info.id, user.sessionId)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
