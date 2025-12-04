package io.stereov.singularity.security.ratelimit

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.security.ratelimit.properties.LoginAttemptLimitProperties
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class LoginAttemptIpLimitFilterTest : BaseIntegrationTest() {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.security.login-attempt-limit.ip-limit") { 2 }
        }
    }

    @Autowired
    private lateinit var loginAttemptLimitProperties: LoginAttemptLimitProperties

    @Test fun `ip rate limit works`() = runTest {
        assertEquals(2, loginAttemptLimitProperties.ipLimit)
        val session = SessionInfoRequest("session")

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest("test@email.com", "password1", session))
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest("test1@email.com", "password1", session))
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest("test2@email.com", "password1", session))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}
