package io.stereov.singularity.user.settings.controller

import io.mockk.verify
import io.stereov.singularity.test.BaseMailIntegrationTest
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class PasswordChangeAlertDisabledTest : BaseMailIntegrationTest() {

    @Test fun `password does not send email`() = runTest {
        val user = registerUser()

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(user.password!!, user.password + "1"))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `email does not send email`() = runTest {
        val user = registerUser()

        webTestClient.put()
            .uri("/api/users/me/email")
            .bodyValue(ChangeEmailRequest("1" + user.email!!))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
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