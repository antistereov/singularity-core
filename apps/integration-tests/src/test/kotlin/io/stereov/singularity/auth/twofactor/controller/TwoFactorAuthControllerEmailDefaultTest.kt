package io.stereov.singularity.auth.twofactor.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class TwoFactorAuthControllerEmailDefaultTest : BaseMailIntegrationTest() {

    @Test fun `email as 2FA should be enabled by default`() = runTest {
        val user = registerUser()

        Assertions.assertTrue(user.info.twoFactorEnabled)
        Assertions.assertEquals(listOf(TwoFactorMethod.EMAIL), user.info.twoFactorMethods)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, user.info.preferredTwoFactorMethod)
        Assertions.assertTrue(user.info.sensitive.security.twoFactor.enabled)
        Assertions.assertTrue(user.info.sensitive.security.twoFactor.email.enabled)
        Assertions.assertFalse(user.info.sensitive.security.twoFactor.totp.enabled)

        val res = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = requireNotNull(res.responseBody)

        Assertions.assertTrue(body.twoFactorRequired)
        Assertions.assertEquals(listOf(TwoFactorMethod.EMAIL), body.allowedTwoFactorMethods)
        Assertions.assertEquals(TwoFactorMethod.EMAIL, body.preferredTwoFactorMethod)

        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }

        val twoFactorAuthToken = res.extractTwoFactorAuthenticationToken()

        Assertions.assertEquals(user.info.id, twoFactorAuthToken.userId)
    }

    @Test fun `login sends 2fa email automatically after login when preferred`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `login does not send 2fa email automatically after login when not preferred`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.TOTP
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `stepUp sends 2fa email automatically after login when preferred`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(StepUpRequest(user.password!!))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `stepUp does not send 2fa email automatically after login when not preferred`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)
        user.info.sensitive.security.twoFactor.preferred = TwoFactorMethod.TOTP
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(StepUpRequest(user.password!!))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.two-factor.email.enable-by-default") { true }
        }
    }
}