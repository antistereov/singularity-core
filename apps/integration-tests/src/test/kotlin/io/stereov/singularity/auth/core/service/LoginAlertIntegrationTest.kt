package io.stereov.singularity.auth.core.service

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.test.BaseSecurityAlertTest
import io.stereov.singularity.user.core.model.AccountDocument
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class LoginAlertIntegrationTest : BaseSecurityAlertTest() {

    @Test fun `works without session and locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val loginUserSlot = slot<AccountDocument>()
        val loginLocaleSlot = slot<Locale?>()
        val loginSessionSlot = slot<SessionInfo>()

        coJustRun { loginAlertService.send(
            capture(loginUserSlot),
            captureNullable(loginLocaleSlot),
            capture(loginSessionSlot)
        ) }

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus()
            .isOk

        val updatedUser = userService.findById(user.info.id)

        coVerify(exactly = 1) { loginAlertService.send(any(), anyNullable(), anyNullable()) }
        assert(loginUserSlot.isCaptured)
        assertEquals(user.info.id, loginUserSlot.captured.id)
        assert(loginLocaleSlot.isNull)
        assertEquals(updatedUser.sensitive.sessions.values.first(), loginSessionSlot.captured)
    }
    @Test fun `works with session and locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val loginUserSlot = slot<AccountDocument>()
        val loginLocaleSlot = slot<Locale>()
        val loginSessionSlot = slot<SessionInfo>()

        coJustRun { loginAlertService.send(
            capture(loginUserSlot),
            capture(loginLocaleSlot),
            capture(loginSessionSlot)
        ) }

        webTestClient.post()
            .uri("/api/auth/login?locale=en")
            .bodyValue(LoginRequest(user.email!!, user.password!!, SessionInfoRequest("browser", "os")))
            .exchange()
            .expectStatus()
            .isOk

        val updatedUser = userService.findById(user.info.id)

        coVerify(exactly = 1) { loginAlertService.send(any(), any(), any()) }
        assert(loginUserSlot.isCaptured)
        assertEquals(user.info.id, loginUserSlot.captured.id)
        assertEquals(Locale.ENGLISH, loginLocaleSlot.captured)
        assertEquals(updatedUser.sensitive.sessions.values.first(), loginSessionSlot.captured)
        assertEquals("browser", loginSessionSlot.captured.browser)
        assertEquals("os", loginSessionSlot.captured.os)
    }
}