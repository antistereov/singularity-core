package io.stereov.singularity.test

import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.test.config.MockMailSenderConfig
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthService
import io.stereov.singularity.auth.token.service.TwoFactorTokenService
import io.stereov.singularity.auth.token.service.AccessTokenService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@Import(MockMailSenderConfig::class)
class BaseIntegrationTest : BaseSpringBootTest() {

    final val basePath = "/api"

    @Autowired
    lateinit var accessTokenService: AccessTokenService

    @Autowired
    lateinit var twoFactorAuthService: TwoFactorAuthService

    @Autowired
    lateinit var twoFactorTokenService: TwoFactorTokenService

    @Autowired
    lateinit var hashService: HashService

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
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("singularity.security.rate-limit.user-limit") { 10000 }
        }
    }
}
