package io.stereov.singularity.auth.twofactor

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseSecurityAlertTest
import io.stereov.singularity.user.core.model.User
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class TwoFactorAlertUnitTest : BaseSecurityAlertTest() {

    @Test fun `enable email works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val userSlot = slot<User>()
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

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(EnableEmailTwoFactorMethodRequest(user.email2faCode))
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.TWO_FACTOR_ADDED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.EMAIL, twoFactorMethodSlot.captured)
    }
    @Test fun `enable email works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val userSlot = slot<User>()
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

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(EnableEmailTwoFactorMethodRequest(user.email2faCode))
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assertEquals(SecurityAlertType.TWO_FACTOR_ADDED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.EMAIL, twoFactorMethodSlot.captured)
    }

    @Test fun `disable email works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        val userSlot = slot<User>()
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

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.TWO_FACTOR_REMOVED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.EMAIL, twoFactorMethodSlot.captured)
    }
    @Test fun `disable email works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        val userSlot = slot<User>()
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

        webTestClient.delete()
            .uri("/api/auth/2fa/email?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assertEquals(SecurityAlertType.TWO_FACTOR_REMOVED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.EMAIL, twoFactorMethodSlot.captured)
    }

    @Test fun `enable totp works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val userSlot = slot<User>()
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

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.TWO_FACTOR_ADDED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.TOTP, twoFactorMethodSlot.captured)
    }
    @Test fun `enable totp works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = false
        userService.save(user.info)

        val userSlot = slot<User>()
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

        val secret = totpService.generateSecretKey()
        val recoveryCode = Random.generateString(10)
        val token = setupTokenService.create(user.info.id, secret, listOf(recoveryCode))
        val code = gAuth.getTotpPassword(secret)

        webTestClient.post()
            .uri("/api/auth/2fa/totp/setup?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(TwoFactorVerifySetupRequest(token.value, code))
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assertEquals(SecurityAlertType.TWO_FACTOR_ADDED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.TOTP, twoFactorMethodSlot.captured)
    }

    @Test fun `disable totp works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        val userSlot = slot<User>()
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

        webTestClient.delete()
            .uri("/api/auth/2fa/totp")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.TWO_FACTOR_REMOVED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.TOTP, twoFactorMethodSlot.captured)
    }
    @Test fun `disable totp works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.twoFactor.email.enabled = true
        user.info.sensitive.security.twoFactor.totp.enabled = true
        userService.save(user.info)

        val userSlot = slot<User>()
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

        webTestClient.delete()
            .uri("/api/auth/2fa/totp?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assertEquals(SecurityAlertType.TWO_FACTOR_REMOVED, alertTypeSlot.captured)
        assertEquals(TwoFactorMethod.TOTP, twoFactorMethodSlot.captured)
    }
}
