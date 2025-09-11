package io.stereov.singularity.auth.core

import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.test.BaseSpringBootTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class HeaderAuthenticationPreferHeaderTest : BaseSpringBootTest() {

    companion object {
        val mongoDBContainer = MongoDBContainer("mongo:latest").apply {
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
            registry.add("singularity.auth.allow-header-authentication") { true }
            registry.add("singularity.auth.prefer-header-authentication") { true }
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }

    @Test fun `should prefer header token with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, "invalid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `should prefer header token with invalid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `should prefer header token and fall back to cookie token with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `should prefer header token and fall back to cookie token with invalid token`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, "invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
