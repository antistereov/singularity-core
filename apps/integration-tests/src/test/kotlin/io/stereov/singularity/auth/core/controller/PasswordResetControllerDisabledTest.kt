package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class PasswordResetControllerDisabledTest : BaseIntegrationTest() {

    @Test
    fun `sendPasswordReset throws disabled exception`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email!!))
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }
}