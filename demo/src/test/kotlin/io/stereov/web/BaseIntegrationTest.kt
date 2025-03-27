package io.stereov.web

import io.stereov.web.config.Constants
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.user.dto.DeviceInfoRequest
import io.stereov.web.user.dto.LoginRequest
import io.stereov.web.user.dto.RegisterUserRequest
import io.stereov.web.user.dto.TwoFactorSetupResponse
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.service.UserService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BaseIntegrationTest {

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var jwtService: JwtService

    @AfterEach
    fun clearDatabase() = runBlocking {
        userService.deleteAll()
    }

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
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("webstarter.app.name") { "TestApplication" }
            registry.add("webstarter.security.jwt.expires-in") { 900 }
            registry.add("webstarter.security.jwt.secret-key") { "64f09a172d31b6253d0af2e7dccce6bc9e4e55f8043df07c3ebda72c262758662c2c076e9f11965f43959186b9903fa122da44699b38e40ec21b4bd2fc0ad8c93be946d3dcd0208a1a3ae9d39d4482674d56f6e6dddfe8a6321ad31a824b26e3d528943b0943ad3560d23a79da1fefde0ee2a20709437cedee9def79d5b4c0cf96ee36c02b67ab5fd28638606a5c19ffe8b76d40077549f6db6920a97da0089f5cd2d28665e1d4fb6d9a68fe7b78516a8fc8c33d6a6dac53a77ab670e3449cb237a49104478b717e20e1d22e270f7cf06f9b412b55255c150cb079365eadaddd319385d6221e4b40ed89cdcde540538ce88e66ae2f783c3c48859a14ec6eff83" }
            registry.add("webstarter.security.encryption.secret-key") { "3eJAiq7XBjMc5AXkCwsjbA==" }
            registry.add("webstarter.mail.enable-email-verification") { false }
            registry.add("webstarter.mail.host") { "host.com" }
            registry.add("webstarter.mail.port") { "587" }
            registry.add("webstarter.mail.email") { "mail@host.com" }
            registry.add("webstarter.mail.username") { "mail@host.com" }
            registry.add("webstarter.mail.password") { "mailpassword"}
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
        }
    }

    data class TestRegisterResponse(
        val info: UserDocument,
        val accessToken: String,
        val refreshToken: String,
        val twoFactorToken: String?,
        val twoFactorSecret: String?,
        val twoFactorRecovery: String?,
    )

    suspend fun registerUser(
        email: String = "test@email.com",
        password: String = "password",
        deviceId: String = "device",
        twoFactorEnabled: Boolean = false,
    ): TestRegisterResponse {
        val device = DeviceInfoRequest(id = deviceId)

        val responseCookies = webTestClient.post()
            .uri("/user/register")
            .bodyValue(RegisterUserRequest(email = email, password = password, device = device))
            .exchange()
            .expectStatus().isOk
            .returnResult<Void>()
            .responseCookies

        val accessToken = responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
        val refreshToken = responseCookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token contained in response" }
        requireNotNull(refreshToken) { "No refresh token contained in response" }

        var twoFactorToken: String? = null
        var twoFactorRecovery: String? = null
        var twoFactorSecret: String? = null

        if (twoFactorEnabled) {
            val twoFactorRes = webTestClient.post()
                .uri("/user/2fa/setup")
                .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
                .exchange()
                .expectStatus().isOk
                .expectBody(TwoFactorSetupResponse::class.java)
                .returnResult()
                .responseBody

            twoFactorRecovery = twoFactorRes?.recoveryCode
            twoFactorSecret = twoFactorRes?.secret

            twoFactorToken = webTestClient.post()
                .uri("/user/login")
                .bodyValue(LoginRequest(email, password, device))
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies[Constants.TWO_FACTOR_AUTH_COOKIE]
                ?.firstOrNull()
                ?.value
        }


        val user = userService.findByEmailOrNull(email)
        requireNotNull(user) { "User associated to $email not saved" }

        return TestRegisterResponse(user, accessToken, refreshToken, twoFactorToken, twoFactorSecret, twoFactorRecovery)
    }

    suspend fun deleteAccount(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/user/me")
            .header(HttpHeaders.COOKIE, "${Constants.ACCESS_TOKEN_COOKIE}=${response.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
}
