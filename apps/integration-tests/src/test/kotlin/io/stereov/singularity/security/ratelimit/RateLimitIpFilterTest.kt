package io.stereov.singularity.security.ratelimit

import io.stereov.singularity.security.ratelimit.properties.RateLimitProperties
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class RateLimitIpFilterTest : BaseIntegrationTest() {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.security.rate-limit.user-limit") { 4 }
            registry.add("singularity.security.rate-limit.ip-limit") { 2 }
        }
    }

    @Autowired
    private lateinit var rateLimitProperties: RateLimitProperties

    @Test fun `ip rate limit works`() = runTest {
        assertEquals(2, rateLimitProperties.ipLimit)

        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}
