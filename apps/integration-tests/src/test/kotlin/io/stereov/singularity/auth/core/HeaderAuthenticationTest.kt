package io.stereov.singularity.auth.core.core

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.test.BaseSpringBootTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant

class HeaderAuthenticationTest : BaseSpringBootTest() {

    @Autowired
    lateinit var accessTokenService: AccessTokenService
    
    @Autowired
    lateinit var refreshTokenService: RefreshTokenService

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
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `valid token required needs bearer prefix`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `valid token required`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "access_token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token and user account required`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `unexpired token required`() = runTest {
        val user = registerUser()
        val token = accessTokenService.create(user.info.id, "session", Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, token.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/sessions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `invalid session will not be authorized`() = runTest {
        val user = registerUser(sessionId = "session")
        val accessToken = accessTokenService.create(user.info.id, "session")

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken.value}")
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.id)
        foundUser.sensitive.sessions.clear()
        userService.save(foundUser)

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken.value}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `refresh works`() = runTest {
        val user = registerUser(sessionId = "session")

        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Refresh.header, "Bearer ${response.accessToken!!}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh does not invalidate old token`() = runTest {
        val user = registerUser(sessionId = "session")

        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Refresh.header, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Refresh.header, "Bearer ${response.accessToken!!}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh requires valid token`() = runTest {
        val user = registerUser(sessionId = "session")

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(SessionTokenType.Refresh.header, "invalid-token")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires unexpired token`() = runTest {
        val user = registerUser(sessionId = "session")
        val token = refreshTokenService.create(user.info.id, "session", user.info.sensitive.sessions.first().refreshTokenId!!,Instant.ofEpochSecond(0))

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(SessionTokenType.Refresh.header, "Bearer ${token.value}")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires token and user account`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .header(SessionTokenType.Refresh.header, "Bearer ${user.accessToken}")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/sessions")
            .header(SessionTokenType.Refresh.header, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh invalid session will not be authorized`() = runTest {
        val user = registerUser(sessionId = "session")
        val accessToken = accessTokenService.create(user.info.id, "session")

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Refresh.header, "Bearer ${accessToken.value}")
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.id)
        foundUser.sensitive.sessions.clear()
        userService.save(foundUser)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
