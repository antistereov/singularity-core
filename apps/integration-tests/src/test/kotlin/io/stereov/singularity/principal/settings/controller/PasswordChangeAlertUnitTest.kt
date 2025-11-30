package io.stereov.singularity.principal.settings.controller

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrThrow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.test.BaseSecurityAlertTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class PasswordChangeAlertUnitTest : BaseSecurityAlertTest() {

    @Test fun `works without locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coEvery { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) } returns Ok(mockk<MimeMessage>())

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(user.password!!, user.password + "1"))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus()
            .isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.id, userSlot.captured.id.getOrThrow())
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.PASSWORD_CHANGED, alertTypeSlot.captured)
    }
    @Test fun `works with locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coEvery { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) } returns Ok(mockk<MimeMessage>())

        webTestClient.put()
            .uri("/api/users/me/password?locale=en")
            .bodyValue(ChangePasswordRequest(user.password!!, user.password + "1"))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus()
            .isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assert(userSlot.isCaptured)
        assertEquals(user.id, userSlot.captured.id.getOrThrow())
        assertEquals(SecurityAlertType.PASSWORD_CHANGED, alertTypeSlot.captured)
    }
}
