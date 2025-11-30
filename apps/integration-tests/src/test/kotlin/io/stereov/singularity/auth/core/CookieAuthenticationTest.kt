package io.stereov.singularity.auth.core

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.jwt.service.JwtSecretService
import io.stereov.singularity.auth.token.model.SessionTokenType
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
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `access valid token required`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie("access_token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token and user account required`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access unexpired token required`() = runTest {
        val user = registerUser()
        val token = accessTokenService.create(user.info, user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(token.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access token gets invalid after logout`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access token gets invalid after logoutAll`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/sessions")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `access with invalid session will not be authorized`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        deleteAllSessions(user)

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
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
            .accessTokenCookie(response.accessToken!!)
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
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(response.accessToken!!)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `refresh requires valid token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, "invalid-token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires unexpired token`() = runTest {
        val user = registerUser()
        val token = refreshTokenService.create(
            user.info.id.getOrThrow(),
            user.sessionId,
            user.info.sensitive.sessions.values.first().refreshTokenId!!,
            Instant.ofEpochSecond(0)).getOrThrow()

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
            .accessTokenCookie(user.accessToken)
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
            .accessTokenCookie(user.accessToken)
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
        val accessToken = accessTokenService.create(user.info, user.sessionId).getOrThrow()

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(cookieCreator.createCookie(accessToken).getOrThrow().value)
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
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `stepUp requires valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie("invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp requires unexpired token`() = runTest {
        val user = registerUser()
        val stepUpToken = stepUpTokenService.create(user.info.id.getOrThrow(), user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpToken.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs valid access token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie("invalid")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs valid unexpired access token`() = runTest {
        val user = registerUser()
        val accessToken = accessTokenService.create(user.info, user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(accessToken.value)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token from matching account`() = runTest {
        val user = registerUser()
        val another = registerUser(emailSuffix = "another@email.com")

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(another.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token from matching session`() = runTest {
        val user = registerUser()
        val stepUpToken = stepUpTokenService.create(user.info.id.getOrThrow(), UUID.randomUUID()).getOrThrow()

        webTestClient.get()
            .uri("/api/auth/2fa/totp/setup")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(stepUpToken.value)
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
