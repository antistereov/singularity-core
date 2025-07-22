package io.stereov.singularity.test

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.test.config.MockMailSenderConfig
import io.stereov.singularity.twofactorauth.service.TwoFactorAuthService
import io.stereov.singularity.user.service.token.TwoFactorAuthTokenService
import io.stereov.singularity.user.service.token.UserTokenService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
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
    lateinit var userTokenService: UserTokenService

    @Autowired
    lateinit var twoFactorAuthService: TwoFactorAuthService

    @Autowired
    lateinit var twoFactorAuthTokenService: TwoFactorAuthTokenService

    @Autowired
    lateinit var hashService: HashService

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var mailSender: JavaMailSender

    @BeforeEach
    fun init() {
        every { mailSender.send(any<SimpleMailMessage>()) } just Runs
    }

    @AfterEach
    fun clearDatabase() = runBlocking {
        userService.deleteAll()
    }

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
