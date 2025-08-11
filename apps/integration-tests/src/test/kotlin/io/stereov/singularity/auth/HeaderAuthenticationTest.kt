package io.stereov.singularity.auth

import io.stereov.singularity.test.BaseSpringBootTest
import io.stereov.singularity.user.device.dto.DeviceInfoRequest
import io.stereov.singularity.user.token.service.AccessTokenService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant

class HeaderAuthenticationTest : BaseSpringBootTest() {

    @Autowired
    lateinit var accessTokenService: AccessTokenService

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
            registry.add("singularity.auth.prefer-header-authentication") { false }
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }

    @Test fun `access with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `valid token required needs bearer prefix`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `valid token required`() = runTest {
        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, "access_token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token and user account required`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `unexpired token required`() = runTest {
        val user = registerUser()
        val token = accessTokenService.createAccessToken(user.info.id, "device", Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/logout-all")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .bodyValue(DeviceInfoRequest(user.info.sensitive.devices.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `invalid device will not be authorized`() = runTest {
        val user = registerUser(deviceId = "device")
        val accessToken = accessTokenService.createAccessToken(user.info.id, "device")

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.id)
        foundUser.sensitive.devices.clear()
        userService.save(foundUser)

        webTestClient.get()
            .uri("/api/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
