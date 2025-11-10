package io.stereov.singularity.user.settings.controller

import io.stereov.singularity.global.model.SendEmailResponse
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserSettingsControllerEmailDisabledTest : BaseIntegrationTest() {

    @Test fun `changeEmail changes email`() = runTest {
        val newEmail = "new@email.com"
        val user = registerUser()

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(SendEmailResponse::class.java)
            .returnResult()
            .responseBody

        val foundUser = userService.findByEmail(newEmail)
        assertEquals(user.info.id, foundUser.id)
    }

}
