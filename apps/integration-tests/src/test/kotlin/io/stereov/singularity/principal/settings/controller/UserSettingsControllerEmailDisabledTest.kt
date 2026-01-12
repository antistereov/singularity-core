package io.stereov.singularity.principal.settings.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.principal.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.principal.settings.dto.response.ChangeEmailResponse
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody

class UserSettingsControllerEmailDisabledTest : BaseIntegrationTest() {

    @Test fun `changeEmail changes email`() = runTest {
        val newEmail = "new@email.com"
        val user = registerUser()

        val res = webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody<ChangeEmailResponse>()
            .returnResult()
            .responseBody

        assertFalse(res!!.verificationRequired)
        val foundUser = userService.findByEmail(newEmail).getOrThrow()
        assertEquals(user.id, foundUser.id.getOrThrow())
    }

}
