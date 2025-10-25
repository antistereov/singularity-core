package io.stereov.singularity.auth.core.controller

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.test.BaseSecurityAlertTest
import io.stereov.singularity.user.core.model.UserDocument
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class RegistrationAlertUnitTest : BaseSecurityAlertTest() {

    @Test fun `works without session and locale`() = runTest {
        val user = registerUser()

        val userSlot = slot<UserDocument>()
        val localeSlot = slot<Locale?>()

        coJustRun { registrationAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(RegisterUserRequest(user.email!!, user.password!!, "Name"))
            .exchange()
            .expectStatus()
            .isOk

        coVerify(exactly = 1) { registrationAlertService.send(any(), anyNullable()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
    }
    @Test fun `works with locale`() = runTest {
        val user = registerUser()

        val userSlot = slot<UserDocument>()
        val localeSlot = slot<Locale?>()

        coJustRun { registrationAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/register?locale=en")
            .bodyValue(RegisterUserRequest(user.email!!, user.password!!, "Name"))
            .exchange()
            .expectStatus()
            .isOk

        coVerify(exactly = 1) { registrationAlertService.send(any(), anyNullable()) }
        assert(userSlot.isCaptured)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
    }
}
