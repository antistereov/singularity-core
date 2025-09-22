package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.dto.response.StepUpResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.request.ChangePreferredTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.CompleteLoginRequest
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.test.BaseMailIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class TwoFactorAuthControllerTest : BaseMailIntegrationTest() {

    @Test fun `verifyLogin requires password auth`() = runTest {
        val user = registerOAuth2()

        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = 123456, email = "123456"))
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin requires 2fa enabled`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = 123456, email = "123456"))
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `verifyLogin with TOTP works`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        val res = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        Assertions.assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.first()
            ?.value
        Assertions.assertEquals("", token)

        Assertions.assertFalse(res.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
        Assertions.assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
    }
    @Test fun `verifyLogin with TOTP saves session data correctly`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val req = CompleteLoginRequest(
            totp = gAuth.getTotpPassword(user.totpSecret),
            session = SessionInfoRequest("browser", "os")
        )

        val accessToken = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(req)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .extractAccessToken()

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertEquals(2, updatedUser.sensitive.sessions.size)
        Assertions.assertEquals(req.session!!.browser, updatedUser.sensitive.sessions[accessToken.sessionId]!!.browser)
        Assertions.assertEquals(req.session!!.os, updatedUser.sensitive.sessions[accessToken.sessionId]!!.os)
    }
    @Test fun `verifyLogin with TOTP needs correct code`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with TOTP needs 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with TOTP needs valid 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie("invalid")
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with TOTP needs unexpired 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id, Instant.ofEpochSecond(0))

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with TOTP needs body`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin with only TOTP needs TOTP code`() = runTest {
        val user = registerUser(totpEnabled = true)
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteLoginRequest(email = "123456"))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin with TOTP returns not modified if already authenticated`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isNotModified
    }
    @Test fun `verifyLogin prefers TOTP`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = code, email = "123456"))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
    }

    @Test fun `verifyLogin with email works`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        val res = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(email = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        Assertions.assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.first()
            ?.value
        Assertions.assertEquals("", token)

        Assertions.assertFalse(res.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
        Assertions.assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
    }
    @Test fun `verifyLogin with email renews code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(email = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertNotEquals(code, updatedUser.sensitive.security.twoFactor.email.code)
        Assertions.assertTrue(user.info.sensitive.security.twoFactor.email.expiresAt.isBefore(updatedUser.sensitive.security.twoFactor.email.expiresAt))
    }
    @Test fun `verifyLogin with email requires unexpired code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        user.info.sensitive.security.twoFactor.email.expiresAt = Instant.ofEpochSecond(0)
        userService.save(user.info)
        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(email = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with email saves session data correctly`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val req = CompleteLoginRequest(
            email = user.info.sensitive.security.twoFactor.email.code,
            session = SessionInfoRequest("browser", "os")
        )

        val res = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(req)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val accessToken = res.extractAccessToken()

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertEquals(2, updatedUser.sensitive.sessions.size)
        Assertions.assertEquals(req.session!!.browser, updatedUser.sensitive.sessions[accessToken.sessionId]!!.browser)
        Assertions.assertEquals(req.session!!.os, updatedUser.sensitive.sessions[accessToken.sessionId]!!.os)
    }
    @Test fun `verifyLogin with email needs correct code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code + 1

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(email = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with email needs 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteLoginRequest(email = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with email needs valid 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie("invalid")
            .bodyValue(CompleteLoginRequest(email = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with email needs unexpired 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)
        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id, Instant.ofEpochSecond(0))

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .bodyValue(CompleteLoginRequest(email = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with email needs body`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin with email needs email code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteLoginRequest(totp = 123456))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin with email returns not modified if already authenticated`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(email = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isNotModified
    }

    @Test fun `verifyStepUp requires password auth`() = runTest {
        val user = registerOAuth2()

        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CompleteStepUpRequest(totp = 123456, email = "123456"))
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp requires 2fa enabled`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(totp = 123456, email = "123456"))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `verifyStepUp with TOTP works`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        val res = webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(totp = code))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        res.extractStepUpToken(user.info.id, user.sessionId)
    }
    @Test fun `verifyStepUp with TOTP needs correct code`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(totp = code))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP needs 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP needs access token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP needs valid 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP needs unexpired 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id, Instant.ofEpochSecond(0))

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP needs 2fa token of same user`() = runTest {
        val user = registerUser(totpEnabled = true)
        val anotherUser = registerUser("another@email.com", totpEnabled = true)
        val twoFactorToken = twoFactorAuthenticationTokenService.create(anotherUser.info.id)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP needs body`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp with only TOTP needs TOTP code`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteLoginRequest(email = "123456"))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp prefers TOTP`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(totp = code, email = "123456"))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
    }

    @Test fun `verifyStepUp with email works`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        val res = webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(email = code))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        res.extractStepUpToken(user.info.id, user.sessionId)
    }
    @Test fun `verifyStepUp with email renews code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(email = code))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertNotEquals(code, updatedUser.sensitive.security.twoFactor.email.code)
        Assertions.assertTrue(user.info.sensitive.security.twoFactor.email.expiresAt.isBefore(updatedUser.sensitive.security.twoFactor.email.expiresAt))
    }
    @Test fun `verifyStepUp with email requires unexpired code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        user.info.sensitive.security.twoFactor.email.expiresAt = Instant.ofEpochSecond(0)
        userService.save(user.info)
        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(email = code))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with email needs correct code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code + 1

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(email = code))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with email needs 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(email = code))
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with email needs access token`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(email = code))
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with email needs valid 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteStepUpRequest(email = code))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie("invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with email needs unexpired 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)
        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id, Instant.ofEpochSecond(0))

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .accessTokenCookie(user.accessToken)
            .bodyValue(CompleteLoginRequest(email = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with email needs 2fa token of same user`() = runTest {
        val user = registerUser(email2faEnabled = true)
        val anotherUser = registerUser("another@email.com", email2faEnabled = true)
        val twoFactorToken = twoFactorAuthenticationTokenService.create(anotherUser.info.id)

        val code = user.info.sensitive.security.twoFactor.email.code

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .bodyValue(CompleteLoginRequest(email = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with email needs body`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp with email needs email code`() = runTest {
        val user = registerUser(email2faEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up")
            .bodyValue(CompleteLoginRequest(totp = 123456))
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `changePreferred works`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        val res = webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(TwoFactorMethod.TOTP, res.preferredTwoFactorMethod)

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.TOTP, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred works when already preferred method`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        val res = webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.EMAIL))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(TwoFactorMethod.EMAIL, res.preferredTwoFactorMethod)

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred is bad when changing to disabled mail`() = runTest {
        val user = registerUser(totpEnabled = true)
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.EMAIL))
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.TOTP, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred is bad when changing to disabled totp`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred is bad when no password authentication`() = runTest {
        val user = registerOAuth2()

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertNull(updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs authentication`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs valid access token`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie("invalid")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs unexpired access token`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie(accessTokenService.create(user.info, user.sessionId, Instant.ofEpochSecond(0)).value)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs step up token`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs valid step up token`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie("invalid")
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs unexpired step up token`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpTokenService.create(user.info.id, user.sessionId, Instant.ofEpochSecond(0)).value)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs step up token of same user`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        val anotherUser = registerUser(emailSuffix = "another@email.com")
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpTokenService.create(anotherUser.info.id, anotherUser.sessionId).value)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
    @Test fun `changePreferred needs step up token of same session`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/preferred-method")
            .bodyValue(ChangePreferredTwoFactorMethodRequest(TwoFactorMethod.TOTP))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpTokenService.create(user.info.id, UUID.randomUUID()).value)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, updatedUser.preferredTwoFactorMethod)
    }
}
