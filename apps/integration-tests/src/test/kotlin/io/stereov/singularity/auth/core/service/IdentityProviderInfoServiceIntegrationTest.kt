package io.stereov.singularity.auth.core.service

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.test.BaseSecurityAlertTest
import io.stereov.singularity.user.core.model.UserDocument
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class IdentityProviderInfoServiceIntegrationTest : BaseSecurityAlertTest() {

    @Test fun `login works without locale`() = runTest {
        val user = registerOAuth2()
        val req = LoginRequest(user.email!!, "Password$2")

        val userSlot = slot<UserDocument>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        coVerify(exactly = 1) { identityProviderInfoService.send(any(), anyNullable()) }
        assert(userSlot.isCaptured)
        Assertions.assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
    }
    @Test fun `login works with locale`() = runTest {
        val user = registerOAuth2()
        val req = LoginRequest(user.email!!, "Password$2")

        val userSlot = slot<UserDocument>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/login?locale=en")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        coVerify(exactly = 1) { identityProviderInfoService.send(any(), anyNullable()) }
        assert(userSlot.isCaptured)
        Assertions.assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isCaptured)
        Assertions.assertEquals(Locale.ENGLISH, localeSlot.captured)
    }
    @Test fun `login does not send for existing user and correct password`() = runTest {
        val user = registerUser()
        val req = LoginRequest(user.email!!, user.password!!)

        val userSlot = slot<UserDocument>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 0) { identityProviderInfoService.send(any(), anyNullable()) }
    }
    @Test fun `login does not send for existing user and wrong password`() = runTest {
        val user = registerUser()
        val req = LoginRequest(user.email!!, "wrong")

        val userSlot = slot<UserDocument>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        coVerify(exactly = 0) { identityProviderInfoService.send(any(), anyNullable()) }
    }
    @Test fun `login does not send for non-existing user`() = runTest {
        val req = LoginRequest("not@email.com", "wrong")

        val userSlot = slot<UserDocument>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        coVerify(exactly = 0) { identityProviderInfoService.send(any(), anyNullable()) }
    }

}