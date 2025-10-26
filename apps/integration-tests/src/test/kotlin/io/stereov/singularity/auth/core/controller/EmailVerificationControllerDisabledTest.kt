package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.SendEmailVerificationRequest
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class EmailVerificationControllerDisabledTest : BaseIntegrationTest() {

    @Test fun `sendVerificationEmail throws disabled exception`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .bodyValue(SendEmailVerificationRequest(user.email!!))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }
}