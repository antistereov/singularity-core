package io.stereov.singularity.auth.core.service

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.model.NoAccountInfoAction
import io.stereov.singularity.test.BaseSecurityAlertTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class NoAccountInfoServiceIntegrationTest : BaseSecurityAlertTest() {

    @Test fun `reset password works without locale`() = runTest {
        val emailSlot = slot<String>()
        val actionSlot = slot<NoAccountInfoAction>()
        val localeSlot = slot<Locale?>()
        val email = "not@existing.com"

        coJustRun { noAccountInfoService.send(
            capture(emailSlot),
            capture(actionSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(email))
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { noAccountInfoService.send(any(), any(), anyNullable()) }
        assert(emailSlot.isCaptured)
        assertEquals(email, emailSlot.captured)
        assert(actionSlot.isCaptured)
        assertEquals(NoAccountInfoAction.PASSWORD_RESET, actionSlot.captured)
        assert(localeSlot.isNull)
    }
    @Test fun `reset password works with locale`() = runTest {
        val emailSlot = slot<String>()
        val actionSlot = slot<NoAccountInfoAction>()
        val localeSlot = slot<Locale?>()
        val email = "not@existing.com"

        coJustRun { noAccountInfoService.send(
            capture(emailSlot),
            capture(actionSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/password/reset-request?locale=en")
            .bodyValue(SendPasswordResetRequest(email))
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { noAccountInfoService.send(any(), any(), anyNullable()) }
        assert(emailSlot.isCaptured)
        assertEquals(email, emailSlot.captured)
        assert(actionSlot.isCaptured)
        assertEquals(NoAccountInfoAction.PASSWORD_RESET, actionSlot.captured)
        assert(localeSlot.isCaptured)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
    }
    @Test fun `reset password does not send for existing`() = runTest {
        val emailSlot = slot<String>()
        val actionSlot = slot<NoAccountInfoAction>()
        val localeSlot = slot<Locale?>()
        val email = registerUser().email!!

        coJustRun { noAccountInfoService.send(
            capture(emailSlot),
            capture(actionSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(email))
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 0) { noAccountInfoService.send(any(), any(), anyNullable()) }
    }
}