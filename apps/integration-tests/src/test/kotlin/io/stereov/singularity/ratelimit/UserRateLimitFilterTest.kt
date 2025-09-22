package io.stereov.singularity.ratelimit

import io.stereov.singularity.ratelimit.properties.RateLimitProperties
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class UserRateLimitFilterTest : BaseIntegrationTest() {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.security.rate-limit.user-limit") { 2 }
            registry.add("singularity.security.rate-limit.ip-limit") { 4 }
        }
    }

    @Autowired
    private lateinit var rateLimitProperties: RateLimitProperties

    @Test fun `account rate limit works`() = runTest {
        val user = registerUser()

        assertEquals(2, rateLimitProperties.userLimit)

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}
