package io.stereov.singularity.auth.core

import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.jwt.service.JwtSecretService
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.util.*

class CookieAuthenticationTest() : BaseIntegrationTest() {

    @Autowired
    private lateinit var jwtDecoder: ReactiveJwtDecoder

    @Autowired
    private lateinit var jwtSecretService: JwtSecretService

    @Test fun `access with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `access valid token required`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, "access_token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token and user account required`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access unexpired token required`() = runTest {
        val user = registerUser()
        val token = accessTokenService.create(user.info, user.sessionId, Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, token.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/sessions")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access with invalid session will not be authorized`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        deleteAllSessions(user)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `refresh works`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, response.accessToken!!)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh does not invalidate old token`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isOk
            .expectBody<RefreshTokenResponse>()
            .returnResult()
            .responseBody

        requireNotNull(response)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, response.accessToken!!)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh requires valid token`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, "invalid-token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires unexpired token`() = runTest {
        val user = registerUser()
        val token = refreshTokenService.create(
            user.info.id,
            user.sessionId,
            user.info.sensitive.sessions.values.first().refreshTokenId!!,
            Instant.ofEpochSecond(0))

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, token.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires token and user account`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/sessions")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh invalid session will not be authorized`() = runTest {
        val user = registerUser()
        val accessToken = accessTokenService.create(user.info, user.sessionId)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, cookieCreator.createCookie(accessToken).value)
            .exchange()
            .expectStatus().isOk

        deleteAllSessions(user)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `stepUp works`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `stepUp requires valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, "invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp requires unexpired token`() = runTest {
        val user = registerUser()
        val stepUpToken = stepUpTokenService.create(user.info.id, user.sessionId, Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, stepUpToken.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs valid access token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, "invalid")
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs valid unexpired access token`() = runTest {
        val user = registerUser()
        val accessToken = accessTokenService.create(user.info, user.sessionId, Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, accessToken.value)
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token from matching account`() = runTest {
        val user = registerUser()
        val another = registerUser(email = "another@email.com")

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, another.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token from matching session`() = runTest {
        val user = registerUser()
        val stepUpToken = stepUpTokenService.create(user.info.id, UUID.randomUUID())

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, stepUpToken.value)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `key rotation works`() = runTest {
        val user = registerUser()

        jwtSecretService.updateSecret()

        val newUser = registerUser("another@email.com")

        val newJwt = jwtDecoder.decode(newUser.accessToken).awaitFirst()
        val newKeyId = newJwt.headers["kid"]


        val jwt = jwtDecoder.decode(user.accessToken).awaitFirst()
        val oldKeyId = jwt.headers["kid"]

        assertNotEquals(oldKeyId, newKeyId)
    }
}
