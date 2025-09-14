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

class SecurityIntegrationTest : BaseIntegrationTest() {

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
    @Test fun `valid token required`() = runTest {
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
    @Test fun `unexpired token required`() = runTest {
        val user = registerUser()
        val token = accessTokenService.create(user.info.id, user.sessionId, Instant.ofEpochSecond(0))

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, token.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token gets invalid after logout`() = runTest {
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
    @Test fun `token gets invalid after logoutAll`() = runTest {
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
    @Test fun `invalid session will not be authorized`() = runTest {
        val user = registerUser()
        val accessToken = accessTokenService.create(user.info.id, user.sessionId)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, cookieCreator.createCookie(accessToken).value)
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.id)
        foundUser.sensitive.sessions.clear()
        userService.save(foundUser)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, cookieCreator.createCookie(accessToken).value)
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
        val accessToken = accessTokenService.create(user.info.id, user.sessionId)

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, cookieCreator.createCookie(accessToken).value)
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findById(user.info.id)
        foundUser.sensitive.sessions.clear()
        userService.save(foundUser)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
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
