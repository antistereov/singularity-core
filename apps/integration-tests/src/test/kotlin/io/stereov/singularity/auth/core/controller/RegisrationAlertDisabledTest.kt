package io.stereov.singularity.auth.core.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class RegisrationAlertDisabledTest : BaseMailIntegrationTest() {

    @Test fun `does not send email`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(RegisterUserRequest(user.email!!, user.password!!, "Name"))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.security-alert.registration-with-existing-email") { false }
        }
    }
}
