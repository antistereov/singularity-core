package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.getOrThrow
import io.mockk.clearMocks
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.test.BaseSecurityAlertTest
import io.stereov.singularity.test.config.MockEmailVerificationService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.util.*

@Import(MockEmailVerificationService::class)
class RegistrationAlertIntegrationTest : BaseSecurityAlertTest() {

    @Autowired
    lateinit var emailVerificationService: EmailVerificationService

    @BeforeEach
    fun initialize() {
        clearMocks(emailVerificationService)
    }

    @Test fun `works without session and locale`() = runTest {
        val user = registerUser()

        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()
        val emailSlot = slot<String>()

        coJustRun { registrationAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        coJustRun { emailVerificationService.startCooldown(capture(emailSlot)) }

        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(RegisterUserRequest(user.email!!, user.password!!, "Name"))
            .exchange()
            .expectStatus()
            .isOk

        coVerify(exactly = 1) { registrationAlertService.send(any(), anyNullable()) }
        coVerify(exactly = 1) { emailVerificationService.startCooldown(any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.id, userSlot.captured.id.getOrThrow())
        assert(localeSlot.isNull)
        assertEquals(user.email, emailSlot.captured)
    }
    @Test fun `does not send when user does not exist`() = runTest {
        val emailSlot = slot<User>()
        val userSlot = slot<User>()
        val registrationLocaleSlot = slot<Locale?>()
        val verificationLocaleSlot = slot<Locale?>()
        val req = RegisterUserRequest("examil@example.com", "Password$1", "Name")

        coJustRun { registrationAlertService.send(
            capture(userSlot),
            captureNullable(registrationLocaleSlot),
        ) }
        coJustRun { emailVerificationService.sendVerificationEmail(
            capture(emailSlot),
            captureNullable(verificationLocaleSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(req)
            .exchange()
            .expectStatus()
            .isOk

        coVerify(exactly = 0) { registrationAlertService.send(any(), anyNullable()) }
        coVerify(exactly = 1) { emailVerificationService.sendVerificationEmail(any(), anyNullable()) }

        assert(emailSlot.isCaptured)
        assertEquals(emailSlot.captured.sensitive.email, req.email)
    }
    @Test fun `works with locale`() = runTest {
        val user = registerUser()

        val userSlot = slot<User>()
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
