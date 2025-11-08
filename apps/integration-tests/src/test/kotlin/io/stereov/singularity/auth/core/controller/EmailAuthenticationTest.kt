package io.stereov.singularity.auth.core.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class EmailAuthenticationTest : BaseMailIntegrationTest() {

    @Test fun `sends 2fa email on login`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `does not throw when cooldown is active for 2fa email on login`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email, user.password))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `sends 2fa email on step-up`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(StepUpRequest( user.password!!))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `does not throw when cooldown is active for 2fa email on step-up`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(StepUpRequest( user.password!!))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(StepUpRequest( user.password))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.security-alert.login") { false }
            registry.add("singularity.auth.security-alert.two-factor-added") { false }
            registry.add("singularity.auth.security-alert.two-factor-removed") { false }
        }
    }
}