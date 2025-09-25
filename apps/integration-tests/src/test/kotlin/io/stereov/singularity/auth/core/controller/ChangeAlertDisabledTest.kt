package io.stereov.singularity.auth.core.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class ChangeAlertDisabledTest : BaseMailIntegrationTest() {

    @Test fun `password reset works without locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret!!)
        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `password reset with locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret!!)
        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token&locale=en")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `email verification works without locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)
        val newEmail = "new@test.com"

        val token = emailVerificationTokenService.create(user.info.id, newEmail,user.mailVerificationSecret!!)
        assertFalse(user.info.sensitive.security.email.verified)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `email verification with locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)
        val newEmail = "new@test.com"

        val token = emailVerificationTokenService.create(user.info.id, newEmail ,user.mailVerificationSecret!!)
        assertFalse(user.info.sensitive.security.email.verified)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token&locale=en")
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.security-alert.password-changed") { false }
            registry.add("singularity.auth.security-alert.email-changed") { false }
        }
    }
}