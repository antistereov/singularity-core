package io.stereov.singularity.user.controller

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.core.dto.response.RegisterResponse
import io.stereov.singularity.auth.core.model.SessionTokenType
import io.stereov.singularity.auth.core.service.EmailVerificationTokenService
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.user.settings.dto.request.ChangeUserRequest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.MultipartBodyBuilder
import java.time.Instant

class UserSessionControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var emailVerificationTokenService: EmailVerificationTokenService

    @Test fun `getAccount returns user account`() = runTest {
        val user = registerUser()

        val responseBody = webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.COOKIE, "${SessionTokenType.Access.cookieName}=${user.accessToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(responseBody) { "Response has empty body" }

        assertEquals(user.info.sensitive.email, responseBody.email)
    }
    @Test fun `getAccount needs authentication`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

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

        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())
        assertEquals(user.info.id, account.id)

        val userResponse = webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userResponse)

        assertEquals(user.info.id, userResponse.id)
        assertEquals(user.info.sensitive.email, userResponse.email)

        assertEquals(sessionId, user.info.sensitive.sessions.firstOrNull()?.id)
        assertEquals(1, user.info.sensitive.sessions.size)

        assertEquals(1, userService.findAll().count())
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

        assertEquals(2, sessions.size)
        assertTrue(sessions.any { it.id == sessionId })
        assertTrue(sessions.any { it.id == newsessionId })
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

        assertTrue(body.twoFactorRequired)
        assertNotNull(body.user)
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

        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())

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

        assertEquals(userDto.id, userDetails.id)
        assertEquals(1, sessions.size)
        assertEquals(sessionId, sessions.first().id)

        assertEquals(1, userService.findAll().count())
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
            .bodyValue(RegisterUserRequest(email = "test@email.com", password = "", name = "Name", session = sessionInfo))
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register needs body`() = runTest {
        webTestClient.post()
            .uri("/api/auth/login")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `changeEmail works with 2fa`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)


        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, stepUpTokenService.create(user.info.id, user.info.sensitive.sessions.first().id).value)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val token = emailVerificationTokenService.create(user.info.id, newEmail, user.mailVerificationSecret)

        val res = webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newEmail, res.email)
        userService.findByEmail(newEmail)
    }
    @Test fun `changeEmail works without 2fa`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val token = emailVerificationTokenService.create(user.info.id, newEmail, user.mailVerificationSecret)

        val res = webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newEmail, res.email)
        userService.findByEmail(newEmail)
    }
    @Test fun `changeEmail changes email`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        val res = webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)
        assertEquals(newEmail, res.email)
        val foundUser = userService.findByEmail(newEmail)
        assertEquals(user.info.id, foundUser.id)
    }
    @Test fun `changeEmail requires authentication`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires body`() = runTest {
        val oldEmail = "old@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `changeEmail requires correct password`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)
        gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, "wrong-password"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, "wrong-password"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same user`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)
        val anotherUser = registerUser("ttest@email.com")

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, stepUpTokenService.create(anotherUser.info.id, user.info.sensitive.sessions.first().id).value)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same session`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, stepUpTokenService.create(user.info.id, "another-session").value)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires unexpired step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                TwoFactorTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.info.id, user.info.sensitive.sessions.first().id, Instant.ofEpochSecond(0)).value
            )
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires valid step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                TwoFactorTokenType.StepUp.cookieName,
                "wrong-token"
            )
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires non-existing email`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)
        registerUser(newEmail)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, password))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
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

    @Test fun `changePassword works with 2fa`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        val res = webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, stepUpTokenService.create(user.info.id, user.info.sensitive.sessions.first().id).value)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email, newPassword, SessionInfoRequest("session")))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `changePassword works without 2fa`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword)

        val res = webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email, newPassword, SessionInfoRequest("session")))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `changePassword requires authentication`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires body`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val user = registerUser(email, oldPassword)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `changePassword requires correct password`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.twoFactorSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangePasswordRequest("wrong-password", newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up token for same user`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)
        val anotherUser = registerUser("another@email.com")

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, stepUpTokenService.create(anotherUser.info.id, user.info.sensitive.sessions.first().id).value)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up token for same session`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, stepUpTokenService.create(user.info.id, "another-session").value)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires unexpired step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, stepUpTokenService.create(user.info.id, user.info.sensitive.sessions.first().id, Instant.ofEpochSecond(0)).value)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires valid step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "password"
        val newPassword = "newPassword"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(TwoFactorTokenType.StepUp.cookieName, "wrong-token")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `changeUser works`() = runTest {
        val user = registerUser()
        val newName = "MyName"
        val accessToken = user.accessToken

        val res = webTestClient.put()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .bodyValue(ChangeUserRequest(newName))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newName, res.name)
        assertEquals(newName, userService.findById(user.info.id).sensitive.name)
    }
    @Test fun `changeUser requires authentication`() = runTest {
        registerUser()
        val newName = "MyName"

        webTestClient.put()
            .uri("/api/users/me")
            .bodyValue(ChangeUserRequest(newName))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeUser requires body`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken

        webTestClient.put()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `setAvatar works`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken

        webTestClient.put()
            .uri("/api/users/me/avatar")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part("file", ClassPathResource("files/test-image.jpg"))
                }.build()
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody
    }

    @Test fun `checkAuthentication requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `checkAuthentication returns user`() = runTest {
        val user = registerUser()

        val response = webTestClient.get()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response) { "Response body is empty" }

        assertEquals(user.info.id, response.id)
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
        val refreshToken = refreshTokenService.create(user.info.id, user.info.sensitive.sessions.first().id, Random.generateCode(20))
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

        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())

        assertEquals(user.info.id, res.user.id)

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

        assertTrue(accessToken.isNullOrBlank())
        assertTrue(refreshToken.isNullOrBlank())

        val account = response.responseBody

        requireNotNull(account) { "No account provided in response" }

        assertTrue(userService.findById(user.info.id).sensitive.sessions.isEmpty())
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

        assertEquals(2, user.sensitive.sessions.size)
        assertTrue(user.sensitive.sessions.any { it.id == sessionId1 })
        assertTrue(user.sensitive.sessions.any { it.id == sessionId2 })

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

        assertTrue(accessToken.isNullOrBlank())
        assertTrue(refreshToken.isNullOrBlank())

        user = userService.findByEmail(email)

        assertTrue(user.sensitive.sessions.isEmpty())
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

        assertTrue(accessToken.isNullOrBlank())
        assertTrue(refreshToken.isNullOrBlank())

        assertEquals(0, userService.findAll().count())
    }
}
