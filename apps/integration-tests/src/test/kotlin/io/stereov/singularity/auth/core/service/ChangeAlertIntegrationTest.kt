package io.stereov.singularity.auth.core.service

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.test.BaseSecurityAlertTest
import io.stereov.singularity.user.core.model.AccountDocument
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.*

class ChangeAlertIntegrationTest : BaseSecurityAlertTest() {

    @Test fun `password reset works without locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val userSlot = slot<AccountDocument>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) }

        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret!!)
        val newPassword = "Password$2"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.PASSWORD_CHANGED, alertTypeSlot.captured)
    }
    @Test fun `password reset with locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val userSlot = slot<AccountDocument>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) }

        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret!!)
        val newPassword = "Password$2"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token&locale=en")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assertEquals(SecurityAlertType.PASSWORD_CHANGED, alertTypeSlot.captured)
    }

    @Test fun `email verification works without locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val userSlot = slot<AccountDocument>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()
        val oldEmailSlot = slot<String?>()
        val newEmailSlot = slot<String?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
            captureNullable(oldEmailSlot),
            captureNullable(newEmailSlot)
        ) }
        val newEmail = "new@test.com"

        val token = emailVerificationTokenService.create(user.info.id, newEmail,user.mailVerificationSecret!!)
        assertFalse(user.info.sensitive.security.email.verified)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable(), anyNullable(), anyNullable()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.EMAIL_CHANGED, alertTypeSlot.captured)
        assertEquals(newEmail, newEmailSlot.captured)
        assertEquals(user.info.sensitive.email!!, oldEmailSlot.captured)
    }
    @Test fun `email verification with locale`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val userSlot = slot<AccountDocument>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()
        val oldEmailSlot = slot<String?>()
        val newEmailSlot = slot<String?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
            captureNullable(oldEmailSlot),
            captureNullable(newEmailSlot)
        ) }
        val newEmail = "new@test.com"

        val token = emailVerificationTokenService.create(user.info.id, newEmail ,user.mailVerificationSecret!!)
        assertFalse(user.info.sensitive.security.email.verified)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token&locale=en")
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable(), anyNullable(), anyNullable()) }
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assertEquals(SecurityAlertType.EMAIL_CHANGED, alertTypeSlot.captured)
        assertEquals(newEmail, newEmailSlot.captured)
        assertEquals(user.info.sensitive.email!!, oldEmailSlot.captured)
    }
}