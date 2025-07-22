package io.stereov.singularity.ratelimit

import io.stereov.singularity.ratelimit.properties.RateLimitProperties
import io.stereov.singularity.test.BaseSpringBootTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class RateLimitIpFilterTest : BaseSpringBootTest() {

    companion object {
        private val mongoDBContainer = MongoDBContainer("mongo:latest").apply {
            start()
        }

        private val redisContainer = GenericContainer(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379)
            .apply {
                start()
            }

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("singularity.security.rate-limit.user-limit") { 4 }
            registry.add("singularity.security.rate-limit.ip-limit") { 2 }
        }
    }

    @Autowired
    private lateinit var rateLimitProperties: RateLimitProperties

    @Test fun `ip rate limit works`() = runTest {
        assertEquals(2, rateLimitProperties.ipLimit)

        webTestClient.get()
            .uri("/api/user/me")
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.get()
            .uri("/api/user/me")
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.get()
            .uri("/api/user/me")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}
