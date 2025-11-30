package io.stereov.singularity.auth.twofactor

import com.github.michaelbull.result.getOrThrow
import io.mockk.verify
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class TwoFactorAlertDisabledTest : BaseMailIntegrationTest() {

    @Test fun `enable email works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)


        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(EnableEmailTwoFactorMethodRequest(user.email2faCode))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `enable email works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(EnableEmailTwoFactorMethodRequest(user.email2faCode))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `disable email works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `disable email works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        webTestClient.delete()
            .uri("/api/auth/2fa/email?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `enable totp works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.info.id.getOrThrow(), secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `enable totp works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val secret = totpService.generateSecretKey().getOrThrow()
        val recoveryCode = Random.generateString(10).getOrThrow()
        val token = setupTokenService.create(user.info.id.getOrThrow(), secret, listOf(recoveryCode)).getOrThrow()
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `disable totp works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `disable totp works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        webTestClient.delete()
            .uri("/api/auth/2fa/totp?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.security-alert.two-factor-added") { false }
            registry.add("singularity.auth.security-alert.two-factor-removed") { false }
        }
    }
}