package io.stereov.singularity.auth.core

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.test.BaseSpringBootTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.containers.GenericContainer
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.*

class HeaderAuthenticationTest : BaseSpringBootTest() {

    @Test fun `access with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `access valid token required needs bearer prefix`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access valid token required`() = runTest {
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
    @Test fun `access unexpired token required`() = runTest {
        val user = registerUser()
        val token = accessTokenService.create(user.info, user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, token.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/users/me/sessions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access invalid session will not be authorized`() = runTest {
        val user = registerUser()
        val accessToken = accessTokenService.create(user.info, user.sessionId).getOrThrow()

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken.value}")
            .exchange()
            .expectStatus().isOk

        deleteAllSessions(user)

        webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken.value}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `refresh works`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Access.header, "Bearer ${response.accessToken!!}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh does not invalidate old token`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Access.header, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Refresh.header, "Bearer ${response.accessToken!!}")
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh requires valid token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(SessionTokenType.Refresh.header, "invalid-token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires unexpired token`() = runTest {
        val user = registerUser()
        val token = refreshTokenService.create(user.id, user.sessionId, user.info.sensitive.sessions.values.first().refreshTokenId!!,Instant.ofEpochSecond(0))
            .getOrThrow()

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(SessionTokenType.Refresh.header, "Bearer ${token.value}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires token and user account`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .header(SessionTokenType.Access.header, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/users/me/sessions")
            .header(SessionTokenType.Access.header, "Bearer ${user.accessToken}")
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh invalid session will not be authorized`() = runTest {
        val user = registerUser()
        val accessToken = accessTokenService.create(user.info, user.sessionId).getOrThrow()

        webTestClient.get()
            .uri("/api/users/me")
            .header(SessionTokenType.Refresh.header, "Bearer ${accessToken.value}")
            .exchange()
            .expectStatus().isOk

        deleteAllSessions(user)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${user.refreshToken}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `stepUp works`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.Access.header, "Bearer ${user.accessToken}")
            .header(SessionTokenType.StepUp.header, user.stepUpToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `stepUp requires valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.Access.header, "Bearer ${user.accessToken}")
            .header(SessionTokenType.StepUp.header, "invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp requires unexpired token`() = runTest {
        val user = registerUser()
        val stepUpToken = stepUpTokenService.create(user.id, user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.Access.header, "Bearer ${user.accessToken}")
            .header(SessionTokenType.StepUp.header, stepUpToken.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.StepUp.header, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs valid access token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.Access.header, "invalid")
            .header(SessionTokenType.StepUp.header, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs valid unexpired access token`() = runTest {
        val user = registerUser()
        val accessToken = accessTokenService.create(user.info, user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.Access.header, "Bearer ${accessToken.value}")
            .header(SessionTokenType.StepUp.header, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token from matching account`() = runTest {
        val user = registerUser()
        val another = registerUser(emailSuffix = "another@email.com")

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.Access.header, "Bearer ${another.accessToken}")
            .header(SessionTokenType.StepUp.header, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token from matching session`() = runTest {
        val user = registerUser()
        val stepUpToken = stepUpTokenService.create(user.id, UUID.randomUUID()).getOrThrow()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .header(SessionTokenType.Access.header, "Bearer ${user.accessToken}")
            .header(SessionTokenType.StepUp.header, stepUpToken.value)
            .exchange()
            .expectStatus().isUnauthorized
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
            registry.add("singularity.auth.allow-header-authentication") { true }
            registry.add("singularity.auth.prefer-header-authentication") { false }
            registry.add("spring.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }
}
