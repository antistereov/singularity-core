package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class EmailAuthenticationControllerDisabledTest : BaseIntegrationTest() {

    @Test fun `send 2FA email should throw service unavailable`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }
}