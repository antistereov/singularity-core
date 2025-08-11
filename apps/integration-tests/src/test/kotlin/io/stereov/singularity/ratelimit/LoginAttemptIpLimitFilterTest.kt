package io.stereov.singularity.ratelimit

import io.stereov.singularity.ratelimit.properties.LoginAttemptLimitProperties
import io.stereov.singularity.test.BaseSpringBootTest
import io.stereov.singularity.user.device.dto.DeviceInfoRequest
import io.stereov.singularity.user.session.dto.request.LoginRequest
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

class LoginAttemptIpLimitFilterTest : BaseSpringBootTest() {

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
            registry.add("singularity.security.login-attempt-limit.ip-limit") { 2 }
        }
    }

    @Autowired
    private lateinit var loginAttemptLimitProperties: LoginAttemptLimitProperties

    @Test fun `ip rate limit works`() = runTest {
        assertEquals(2, loginAttemptLimitProperties.ipLimit)
        val device = DeviceInfoRequest("device")

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest("test@email.com", "password1", device))
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest("test1@email.com", "password1", device))
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(LoginRequest("test2@email.com", "password1", device))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }
}
