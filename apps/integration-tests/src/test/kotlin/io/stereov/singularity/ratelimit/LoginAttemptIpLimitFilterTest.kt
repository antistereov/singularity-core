package io.stereov.singularity.ratelimit

import io.stereov.singularity.ratelimit.properties.LoginAttemptLimitProperties
import io.stereov.singularity.secrets.core.properties.SecretStoreImplementation
import io.stereov.singularity.test.BaseSpringBootTest
import io.stereov.singularity.user.dto.request.DeviceInfoRequest
import io.stereov.singularity.user.dto.request.LoginRequest
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
import java.util.*

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
            registry.add("singularity.app.support-email") { "support@example.com" }
            registry.add("singularity.app.create-root-user") { "false" }
            registry.add("singularity.secrets.store") { SecretStoreImplementation.MOCK }
            registry.add("singularity.secrets.bitwarden.api-url") { "https//api.bitwarden.com" }
            registry.add("singularity.secrets.bitwarden.identity-url") { "https//identity.bitwarden.com" }
            registry.add("singularity.secrets.bitwarden.organization-id") { UUID.randomUUID() }
            registry.add("singularity.secrets.bitwarden.project-id") { UUID.randomUUID() }
            registry.add("singularity.secrets.bitwarden.access-token") { "asghaosg" }
            registry.add("singularity.secrets.bitwarden.state-file") { "asghaosg" }
            registry.add("singularity.file.storage.s3.domain") { "amazon.com" }
            registry.add("singularity.file.storage.s3.access-key") { "amazon.com" }
            registry.add("singularity.file.storage.s3.secret-key") { "amazon.com" }
            registry.add("singularity.file.storage.s3.scheme") { "https" }
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("singularity.app.name") { "TestApplication" }
            registry.add("singularity.security.jwt.expires-in") { 900 }
            registry.add("singularity.security.jwt.secret-key") { "64f09a172d31b6253d0af2e7dccce6bc9e4e55f8043df07c3ebda72c262758662c2c076e9f11965f43959186b9903fa122da44699b38e40ec21b4bd2fc0ad8c93be946d3dcd0208a1a3ae9d39d4482674d56f6e6dddfe8a6321ad31a824b26e3d528943b0943ad3560d23a79da1fefde0ee2a20709437cedee9def79d5b4c0cf96ee36c02b67ab5fd28638606a5c19ffe8b76d40077549f6db6920a97da0089f5cd2d28665e1d4fb6d9a68fe7b78516a8fc8c33d6a6dac53a77ab670e3449cb237a49104478b717e20e1d22e270f7cf06f9b412b55255c150cb079365eadaddd319385d6221e4b40ed89cdcde540538ce88e66ae2f783c3c48859a14ec6eff83" }
            registry.add("singularity.security.encryption.secret-key") { "3eJAiq7XBjMc5AXkCwsjbA==" }
            registry.add("singularity.mail.enable") { true }
            registry.add("singularity.mail.host") { "host.com" }
            registry.add("singularity.mail.port") { "587" }
            registry.add("singularity.mail.email") { "mail@host.com" }
            registry.add("singularity.mail.username") { "mail@host.com" }
            registry.add("singularity.mail.password") { "mailpassword"}
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
            registry.add("singularity.security.rate-limit.user-limit") { 10000 }
            registry.add("singularity.security.rate-limit.user-time-window") { 2 }
            registry.add("singularity.security.rate-limit.ip-limit") { 10000 }
            registry.add("singularity.security.rate-limit.ip-time-window") { 2 }
            registry.add("singularity.security.login-attempt-limit.ip-limit") { 2 }
            registry.add("singularity.security.login-attempt-limit.ip-time-window") { 1 }

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
