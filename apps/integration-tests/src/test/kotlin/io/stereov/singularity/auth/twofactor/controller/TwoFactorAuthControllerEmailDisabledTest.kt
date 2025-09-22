package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class TwoFactorAuthControllerEmailDisabledTest : BaseIntegrationTest() {

    @Test fun `2FA is not enabled by default`() = runTest {
        val user = registerUser()

        Assertions.assertFalse(user.info.twoFactorEnabled)
        Assertions.assertEquals(listOf<TwoFactorMethod>(), user.info.twoFactorMethods)
        Assertions.assertNull(user.info.preferredTwoFactorMethod)
        Assertions.assertFalse(user.info.sensitive.security.twoFactor.enabled)
        Assertions.assertFalse(user.info.sensitive.security.twoFactor.email.enabled)
        Assertions.assertFalse(user.info.sensitive.security.twoFactor.totp.enabled)

        val res = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = requireNotNull(res.responseBody)

        Assertions.assertFalse(body.twoFactorRequired)
        Assertions.assertNull(body.allowedTwoFactorMethods)
        Assertions.assertNull(body.preferredTwoFactorMethod)

        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()

        assertThrows<TokenException> { res.extractTwoFactorAuthenticationToken() }

        Assertions.assertEquals(user.info.id, accessToken.userId)
        Assertions.assertEquals(user.info.id, refreshToken.userId)
        Assertions.assertEquals(accessToken.sessionId, refreshToken.sessionId)

    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.two-factor.email.enable-by-default") { true }
        }
    }
}