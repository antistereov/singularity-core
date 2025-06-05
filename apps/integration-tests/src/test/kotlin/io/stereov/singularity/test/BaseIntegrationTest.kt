package io.stereov.singularity.test

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.stereov.singularity.cache.service.AccessTokenCache
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.secrets.properties.KeyManagerImplementation
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
    lateinit var accessTokenCache: AccessTokenCache

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
            registry.add("singularity.app.support-email") { "support@example.com" }
            registry.add("singularity.app.create-root-user") { "false" }
            registry.add("singularity.secrets.key-manager") { KeyManagerImplementation.Mock }
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
            registry.add("singularity.security.rate-limit.user-time-window") { 1 }
            registry.add("singularity.security.rate-limit.ip-limit") { 10000 }
            registry.add("singularity.security.rate-limit.ip-time-window") { 1 }
            registry.add("singularity.security.login-attempt-limit.ip-limit") { 10000 }
            registry.add("singularity.security.login-attempt-limit.ip-time-window") { 1 }
        }
    }
}
