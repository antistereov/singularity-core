package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.core.dto.response.RegisterResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AuthenticationControllerTest : BaseIntegrationTest() {

    @Test fun `login logs in user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val sessionId = "session"
        val user = registerUser(email, password, sessionId)
        val loginRequest = LoginRequest(email, password, SessionInfoRequest(sessionId))

        val response = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val accessToken = response.responseCookies[SessionTokenType.Access.cookieName]
            ?.firstOrNull()?.value
        val refreshToken = response.responseCookies[SessionTokenType.Refresh.cookieName]
            ?.firstOrNull()?.value
        val account = response.responseBody?.user

        requireNotNull(accessToken) { "No access token provided in response" }
        requireNotNull(refreshToken) { "No refresh token provided in response" }
        requireNotNull(account) { "No auth info provided in response" }

        Assertions.assertTrue(accessToken.isNotBlank())
        Assertions.assertTrue(refreshToken.isNotBlank())
        Assertions.assertEquals(user.info.id, account.id)

        val userResponse = webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userResponse)

        Assertions.assertEquals(user.info.id, userResponse.id)
        Assertions.assertEquals(user.info.sensitive.email, userResponse.email)

        Assertions.assertEquals(sessionId, user.info.sensitive.sessions.firstOrNull()?.id)
        Assertions.assertEquals(1, user.info.sensitive.sessions.size)

        Assertions.assertEquals(1, userService.findAll().count())
    }
    @Test fun `login needs body`() = runTest {
        webTestClient.post()
            .uri("/api/auth/login")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `login needs correct body`() = runTest {
        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue("Test")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }
    @Test fun `login needs valid credentials`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(
                LoginRequest(
                    user.info.sensitive.email,
                    "wrong password",
                    user.info.sensitive.sessions.first().toRequestDto()
                )
            )
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(
                LoginRequest(
                    "another@email.com",
                    "wrong password",
                    user.info.sensitive.sessions.first().toRequestDto()
                )
            )
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `login from new session saves session`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val sessionId = "session"
        val newsessionId = "newsessionId"

        val user = registerUser(email, password, sessionId)

        val accessToken = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email, password, SessionInfoRequest(newsessionId)))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token provided in response" }

        val userInfo = webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userInfo) { "No UserDetails provided in response" }

        val sessions = userService.findById(user.info.id).sensitive.sessions

        Assertions.assertEquals(2, sessions.size)
        Assertions.assertTrue(sessions.any { it.id == sessionId })
        Assertions.assertTrue(sessions.any { it.id == newsessionId })
    }
    @Test fun `login with two factor works as expected`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val sessionId = "session"

        val user = registerUser(email, password, sessionId, true)

        val loginRequest = LoginRequest(email, password, SessionInfoRequest(sessionId))

        val response = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = response.responseBody
        requireNotNull(body)

        requireNotNull(user.twoFactorToken)

        Assertions.assertTrue(body.twoFactorRequired)
        Assertions.assertNotNull(body.user)
    }

    @Test fun `register registers new user`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val sessionId = "session"
        val sessionInfo = SessionInfoRequest(id = sessionId)

        val response = webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(RegisterUserRequest(email = email, password = password, name = "Name", session = sessionInfo))
            .exchange()
            .expectStatus().isOk
            .expectBody(RegisterResponse::class.java)
            .returnResult()

        val accessToken = response.responseCookies[SessionTokenType.Access.cookieName]
            ?.firstOrNull()?.value
        val refreshToken = response.responseCookies[SessionTokenType.Refresh.cookieName]
            ?.firstOrNull()?.value
        val userDto = response.responseBody!!.user

        requireNotNull(accessToken) { "No access token provided in response" }
        requireNotNull(refreshToken) { "No refresh token provided in response" }
        requireNotNull(userDto) { "No user info provided in response" }

        Assertions.assertTrue(accessToken.isNotBlank())
        Assertions.assertTrue(refreshToken.isNotBlank())

        val userDetails = webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userDetails) { "No UserDetails provided in response" }

        val sessions = userService.findById(userDto.id).sensitive.sessions

        Assertions.assertEquals(userDto.id, userDetails.id)
        Assertions.assertEquals(1, sessions.size)
        Assertions.assertEquals(sessionId, sessions.first().id)

        Assertions.assertEquals(1, userService.findAll().count())
    }
    @Test fun `register requires valid credentials`() = runTest {
        val sessionInfo = SessionInfoRequest("session")
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(
                RegisterUserRequest(
                    email = "invalid",
                    password = "password",
                    name = "Name",
                    session = sessionInfo
                )
            )
            .exchange()
            .expectStatus().isBadRequest

        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(RegisterUserRequest(email = "", password = "password", name = "Name", session = sessionInfo))
            .exchange()
            .expectStatus().isBadRequest

        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(
                RegisterUserRequest(
                    email = "test@email.com",
                    password = "",
                    name = "Name",
                    session = sessionInfo
                )
            )
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register needs body`() = runTest {
        webTestClient.post()
            .uri("/api/auth/login")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `sendVerificationEmail throws disabled exception`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/email/verify/send")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }
    @Test fun `sendPasswordReset throws disabled exception`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email))
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test fun `refresh requires body`() = runTest {
        webTestClient.post()
            .uri("/api/auth/refresh")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `refresh requires token`() = runTest {
        val sessionInfo = SessionInfoRequest("session")
        webTestClient.post()
            .uri("/api/auth/refresh")
            .bodyValue(sessionInfo)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires valid token`() = runTest {
        val sessionInfo = SessionInfoRequest("session")
        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, "Refresh")
            .bodyValue(sessionInfo)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh requires associated token to account`() = runTest {
        val user = registerUser()
        val refreshToken = refreshTokenService.create(
            user.info.id,
            user.info.sensitive.sessions.first().id,
            Random.generateString(20)
        )
        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, refreshToken.value)
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token is valid once`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.firstOrNull()?.id!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token requires associated session`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .bodyValue(SessionInfoRequest("another session"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh returns valid tokens`() = runTest {
        val user = registerUser()
        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isOk
            .expectBody(RefreshTokenResponse::class.java)
            .returnResult()

        val res = response.responseBody
        val accessToken = response.responseCookies[SessionTokenType.Access.cookieName]
            ?.firstOrNull()?.value
        val refreshToken = response.responseCookies[SessionTokenType.Refresh.cookieName]
            ?.firstOrNull()?.value

        requireNotNull(res) { "No account provided in response" }
        requireNotNull(accessToken) { "No access token provided in response" }
        requireNotNull(refreshToken) { "No refresh token provided in response" }

        Assertions.assertTrue(accessToken.isNotBlank())
        Assertions.assertTrue(refreshToken.isNotBlank())

        Assertions.assertEquals(user.info.id, res.user.id)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, refreshToken)
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .exchange()
            .expectStatus().isOk
    }

    @Test fun `logout requires body`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/logout")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `logout deletes all cookies and logs out user`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/api/auth/logout")
            .bodyValue(SessionInfoRequest(user.info.sensitive.sessions.first().id))
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        val refreshToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value

        Assertions.assertTrue(accessToken.isNullOrBlank())
        Assertions.assertTrue(refreshToken.isNullOrBlank())

        val account = response.responseBody

        requireNotNull(account) { "No account provided in response" }

        Assertions.assertTrue(userService.findById(user.info.id).sensitive.sessions.isEmpty())
    }
    @Test fun `logout requires authentication`() = runTest {
        webTestClient.post()
            .uri("/api/auth/logout")
            .bodyValue(SessionInfoRequest("session"))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `logoutAllsessions works`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val sessionId1 = "session"
        val sessionId2 = "session2"
        val registeredUser = registerUser(email, password, sessionId1)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email, password, SessionInfoRequest(sessionId1)))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email, password, SessionInfoRequest(sessionId2)))
            .exchange()
            .expectStatus().isOk

        var user = userService.findByEmail(email)

        Assertions.assertEquals(2, user.sensitive.sessions.size)
        Assertions.assertTrue(user.sensitive.sessions.any { it.id == sessionId1 })
        Assertions.assertTrue(user.sensitive.sessions.any { it.id == sessionId2 })

        val response = webTestClient.delete()
            .uri("/api/auth/sessions")
            .cookie(SessionTokenType.Access.cookieName, registeredUser.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        val refreshToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value

        Assertions.assertTrue(accessToken.isNullOrBlank())
        Assertions.assertTrue(refreshToken.isNullOrBlank())

        user = userService.findByEmail(email)

        Assertions.assertTrue(user.sensitive.sessions.isEmpty())
    }
    @Test fun `logoutAllsessions requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/auth/sessions")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `delete requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `delete deletes all cookies and deletes user`() = runTest {
        val user = registerUser()

        val response = webTestClient.delete()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        val refreshToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value

        Assertions.assertTrue(accessToken.isNullOrBlank())
        Assertions.assertTrue(refreshToken.isNullOrBlank())

        Assertions.assertEquals(0, userService.findAll().count())
    }
}