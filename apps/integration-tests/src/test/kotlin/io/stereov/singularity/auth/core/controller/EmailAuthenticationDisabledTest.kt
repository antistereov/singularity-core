package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class EmailAuthenticationDisabledTest : BaseIntegrationTest() {

    @Test fun `does not throw for 2fa email on login`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk

    }

    @Test fun `does not throw for  2fa email on step-up`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(StepUpRequest(user.password!!))
            .exchange()
            .expectStatus().isOk
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