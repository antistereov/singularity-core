package io.stereov.singularity.ratelimit

import io.stereov.singularity.ratelimit.properties.LoginAttemptLimitProperties
import io.stereov.singularity.secrets.properties.KeyManagerImplementation
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
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("baseline.app.support-email") { "support@example.com" }
            registry.add("baseline.app.create-root-user") { "false" }
            registry.add("baseline.secrets.key-manager") { KeyManagerImplementation.Mock }
            registry.add("baseline.secrets.bitwarden.api-url") { "https//api.bitwarden.com" }
            registry.add("baseline.secrets.bitwarden.identity-url") { "https//identity.bitwarden.com" }
            registry.add("baseline.secrets.bitwarden.organization-id") { UUID.randomUUID() }
            registry.add("baseline.secrets.bitwarden.project-id") { UUID.randomUUID() }
            registry.add("baseline.secrets.bitwarden.access-token") { "asghaosg" }
            registry.add("baseline.secrets.bitwarden.state-file") { "asghaosg" }
            registry.add("baseline.file.storage.s3.domain") { "amazon.com" }
            registry.add("baseline.file.storage.s3.access-key") { "amazon.com" }
            registry.add("baseline.file.storage.s3.secret-key") { "amazon.com" }
            registry.add("baseline.file.storage.s3.scheme") { "https" }
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("baseline.app.name") { "TestApplication" }
            registry.add("baseline.security.jwt.expires-in") { 900 }
            registry.add("baseline.security.jwt.secret-key") { "64f09a172d31b6253d0af2e7dccce6bc9e4e55f8043df07c3ebda72c262758662c2c076e9f11965f43959186b9903fa122da44699b38e40ec21b4bd2fc0ad8c93be946d3dcd0208a1a3ae9d39d4482674d56f6e6dddfe8a6321ad31a824b26e3d528943b0943ad3560d23a79da1fefde0ee2a20709437cedee9def79d5b4c0cf96ee36c02b67ab5fd28638606a5c19ffe8b76d40077549f6db6920a97da0089f5cd2d28665e1d4fb6d9a68fe7b78516a8fc8c33d6a6dac53a77ab670e3449cb237a49104478b717e20e1d22e270f7cf06f9b412b55255c150cb079365eadaddd319385d6221e4b40ed89cdcde540538ce88e66ae2f783c3c48859a14ec6eff83" }
            registry.add("baseline.security.encryption.secret-key") { "3eJAiq7XBjMc5AXkCwsjbA==" }
            registry.add("baseline.mail.enable") { true }
            registry.add("baseline.mail.host") { "host.com" }
            registry.add("baseline.mail.port") { "587" }
            registry.add("baseline.mail.email") { "mail@host.com" }
            registry.add("baseline.mail.username") { "mail@host.com" }
            registry.add("baseline.mail.password") { "mailpassword"}
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
            registry.add("baseline.security.rate-limit.user-limit") { 10000 }
            registry.add("baseline.security.rate-limit.user-time-window") { 2 }
            registry.add("baseline.security.rate-limit.ip-limit") { 10000 }
            registry.add("baseline.security.rate-limit.ip-time-window") { 2 }
            registry.add("baseline.security.login-attempt-limit.ip-limit") { 2 }
            registry.add("baseline.security.login-attempt-limit.ip-time-window") { 1 }

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
