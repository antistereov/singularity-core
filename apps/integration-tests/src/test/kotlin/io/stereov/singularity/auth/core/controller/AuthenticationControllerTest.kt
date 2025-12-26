package io.stereov.singularity.auth.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.auth.core.dto.response.AuthenticationStatusResponse
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.core.dto.response.StepUpResponse
import io.stereov.singularity.auth.token.model.OAuth2TokenType
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.token.model.TwoFactorTokenType
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.principal.core.dto.response.PrincipalResponse
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AuthenticationControllerTest() : BaseIntegrationTest() {

    @Test fun `register registers new user`() = runTest {
        val email = "test@email.com"
        val password = "Password\"3"
        val req = RegisterUserRequest(email = email, password = password, name = "Name")

        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        val userDetails = userService.findByEmail(email).getOrThrow()
        Assertions.assertEquals(userDetails.sensitive.name, req.name)
        Assertions.assertTrue(hashService.checkBcrypt(password, userDetails.password.getOrThrow()).getOrThrow())
        Assertions.assertFalse(userDetails.sensitive.security.email.verified)
        Assertions.assertEquals(1, userService.findAll().getOrThrow().count())
    }
    @Test fun `register requires valid email`() = runTest {
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(
                RegisterUserRequest(
                    email = "invalid",
                    password = "Password$2",
                    name = "Name"
                )
            )
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register requires non-taken email address`() = runTest {
        val user = registerUser()
        val req = RegisterUserRequest(
            email = user.email!!,
            password = "Password$2",
            name = "New Name"
        )
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk()

        val userAfterReq = userService.findById(user.id).getOrThrow()
        Assertions.assertFalse(hashService.checkBcrypt( req.password, userAfterReq.password.getOrThrow()).getOrThrow())
        Assertions.assertNotEquals(userAfterReq.sensitive.name, req.name)
    }
    @Test fun `register requires password of min 8 characters`() = runTest {
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(RegisterUserRequest(email = "", password = "Pas$2", name = "Name"))
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register requires password with lower case letter`() = runTest {
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(
                RegisterUserRequest(
                    email = "test@email.com",
                    password = "PASSWORD$2",
                    name = "Name",
                )
            )
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register requires password with upper-case letter`() = runTest {
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(
                RegisterUserRequest(
                    email = "test@email.com",
                    password = "password$2",
                    name = "Name",
                )
            )
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register requires password with number`() = runTest {
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(
                RegisterUserRequest(
                    email = "test@email.com",
                    password = "Password$",
                    name = "Name",
                )
            )
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `register requires password with special character`() = runTest {
        webTestClient.post()
            .uri("/api/auth/register")
            .bodyValue(
                RegisterUserRequest(
                    email = "test@email.com",
                    password = "Password2",
                    name = "Name",
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
    @Test fun `register returns not modified if already authenticated`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/api/auth/register")
            .accessTokenCookie(user.accessToken)
            .bodyValue(RegisterUserRequest(email = user.email!!, password = user.password!!, name = "Name"))
            .exchange()
            .expectStatus().isNotModified
    }

    @Test fun `login logs in user`() = runTest {
        val user = registerUser()
        val loginRequest = LoginRequest(user.email!!, user.password!!)

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
        Assertions.assertEquals(user.id, account.id)

        val principalResponse = webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(PrincipalResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(principalResponse)

        Assertions.assertEquals(user.id, principalResponse.id)
        Assertions.assertEquals(user.info.sensitive.email, principalResponse.email)

        Assertions.assertEquals(user.sessionId, user.info.sensitive.sessions.keys.first())
        Assertions.assertEquals(1, user.info.sensitive.sessions.size)

        Assertions.assertEquals(1, userService.findAll().getOrThrow().count())
    }
    @Test fun `login should save session correctly`() = runTest {
        val user = registerUser()

        val req = LoginRequest(
            email = user.email!!,
            password = user.password!!,
            session = SessionInfoRequest("browser", "os")
        )

        val result = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val accessToken = result.extractAccessToken()

        val savedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(2, savedUser.sensitive.sessions.size)
        val session = savedUser.sensitive.sessions[accessToken.sessionId]!!

        Assertions.assertEquals(req.session!!.browser, session.browser)
        Assertions.assertEquals(req.session!!.os, session.os)
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
            .bodyValue(LoginRequest(
                user.info.sensitive.email,
                "wrong password"
            ))
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(
                "another@email.com",
                "wrong password"
            ))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `login from new session saves session`() = runTest {
        val user = registerUser()

        val res = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, user.password!!))
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
        val accessToken = res.extractAccessToken()

        val userInfo = webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(accessToken.value)
            .exchange()
            .expectStatus().isOk
            .expectBody(PrincipalResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userInfo) { "No UserDetails provided in response" }

        val sessions = userService.findById(user.id).getOrThrow().sensitive.sessions

        Assertions.assertEquals(2, sessions.size)
        Assertions.assertTrue(sessions.keys.any { it == user.sessionId })
        Assertions.assertTrue { sessions.containsKey(accessToken.sessionId)}
    }
    @Test fun `login with two factor works as expected`() = runTest {
        val user = registerUser(totpEnabled = true)

        val loginRequest = LoginRequest(user.email!!, user.password!!)

        val response = webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val twoFactorToken = response.extractTwoFactorAuthenticationToken()

        val body = response.responseBody
        requireNotNull(body)

        Assertions.assertEquals(twoFactorToken.userId, user.id)
        Assertions.assertTrue(body.twoFactorRequired)
        Assertions.assertNotNull(body.user)
    }
    @Test fun `login returns not modified if already authenticated`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email = user.email!!, password = user.password!!))
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isNotModified
    }
    @Test fun `login for user who authenticated only using OAuth2 is bad`() = runTest {
        val user = registerOAuth2()

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(
                user.info.sensitive.email,
                "any password"
            ))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `logout deletes all cookies and logs out user`() = runTest {
        val user = registerUser()

        val response = webTestClient.post()
            .uri("/api/auth/logout")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        val refreshToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        val sessionToken = cookies[SessionTokenType.Session.cookieName]?.firstOrNull()?.value
        val stepUpToken = cookies[SessionTokenType.StepUp.cookieName]?.firstOrNull()?.value
        val twoFactorAuthenticationToken = cookies[TwoFactorTokenType.Authentication.cookieName]?.firstOrNull()?.value
        val oAuth2ProviderConnectionToken = cookies[OAuth2TokenType.ProviderConnection.cookieName]?.firstOrNull()?.value

        Assertions.assertTrue(accessToken.isNullOrBlank())
        Assertions.assertTrue(refreshToken.isNullOrBlank())
        Assertions.assertTrue(sessionToken.isNullOrBlank())
        Assertions.assertTrue(stepUpToken.isNullOrBlank())
        Assertions.assertTrue(twoFactorAuthenticationToken.isNullOrBlank())
        Assertions.assertTrue(oAuth2ProviderConnectionToken.isNullOrBlank())

        val account = response.responseBody

        requireNotNull(account) { "No account provided in response" }

        Assertions.assertTrue(userService.findById(user.id).getOrThrow().sensitive.sessions.isEmpty())
    }
    @Test fun `logout does not require authentication`() = runTest {
        val response = webTestClient.post()
            .uri("/api/auth/logout")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val cookies = response.responseCookies

        val accessToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        val refreshToken = cookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        val sessionToken = cookies[SessionTokenType.Session.cookieName]?.firstOrNull()?.value
        val stepUpToken = cookies[SessionTokenType.StepUp.cookieName]?.firstOrNull()?.value
        val twoFactorAuthenticationToken = cookies[TwoFactorTokenType.Authentication.cookieName]?.firstOrNull()?.value
        val oAuth2ProviderConnectionToken = cookies[OAuth2TokenType.ProviderConnection.cookieName]?.firstOrNull()?.value

        Assertions.assertTrue(accessToken.isNullOrBlank())
        Assertions.assertTrue(refreshToken.isNullOrBlank())
        Assertions.assertTrue(sessionToken.isNullOrBlank())
        Assertions.assertTrue(stepUpToken.isNullOrBlank())
        Assertions.assertTrue(twoFactorAuthenticationToken.isNullOrBlank())
        Assertions.assertTrue(oAuth2ProviderConnectionToken.isNullOrBlank())

        val account = response.responseBody

        requireNotNull(account) { "No account provided in response" }
    }

    @Test fun `refresh returns valid tokens`() = runTest {
        val user = registerUser()
        val response = webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
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

        Assertions.assertEquals(user.id, res.principal.id)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, refreshToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(accessToken)
            .exchange()
            .expectStatus().isOk
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
        val anotherUser = registerUser(emailSuffix = "another@email.com")
        val refreshToken = refreshTokenService.create(
            anotherUser.id,
            user.sessionId,
            Random.generateString(20).getOrThrow()
        ).getOrThrow()

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, refreshToken.value)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token is valid once`() = runTest {
        val user = registerUser()
        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `refresh token requires associated session`() = runTest {
        val user = registerUser()

        user.info.clearSessions()
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/refresh")
            .cookie(SessionTokenType.Refresh.cookieName, user.refreshToken)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `stepUp works`() = runTest {
        val user = registerUser()
        val req = StepUpRequest(user.password!!)

        val response = webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpResponse::class.java)
            .returnResult()

        val stepUpTokenValue = response.responseCookies[SessionTokenType.StepUp.cookieName]
            ?.firstOrNull()?.value

        requireNotNull(stepUpTokenValue) { "No step up token info provided in response" }

        val stepUpToken = stepUpTokenService.extract(stepUpTokenValue, user.id, user.sessionId).getOrThrow()

        Assertions.assertEquals(user.id, stepUpToken.userId)
    }
    @Test fun `stepUp for user needs body`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `stepUp needs correct body`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue("Test")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }
    @Test fun `stepUp needs valid credentials`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(StepUpRequest(
                "wrong password"
            ))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs valid access token`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/step-up")
            .bodyValue(StepUpRequest(user.password!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp needs access token of same account`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/step-up")
            .bodyValue(StepUpRequest(user.password!!))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUp with two factor works as expected`() = runTest {
        val user = registerUser(totpEnabled = true)

        val req = StepUpRequest(user.password!!)

        val response = webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpResponse::class.java)
            .returnResult()

        val twoFactorToken = response.extractTwoFactorAuthenticationToken()

        val body = response.responseBody
        requireNotNull(body)

        Assertions.assertEquals(twoFactorToken.userId, user.id)
        Assertions.assertTrue(body.twoFactorRequired)
    }
    @Test fun `stepUp for guest works`() = runTest {
        val guest = createGuest()

        val response = webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpResponse::class.java)
            .returnResult()

        val stepUpTokenValue = response.responseCookies[SessionTokenType.StepUp.cookieName]
            ?.firstOrNull()?.value

        requireNotNull(stepUpTokenValue) { "No step up token info provided in response" }

        val stepUpToken = stepUpTokenService.extract(stepUpTokenValue, guest.id, guest.sessionId).getOrThrow()

        Assertions.assertEquals(guest.id, stepUpToken.userId)
    }

    @Test fun `status works with nothing`() = runTest {
        val res = webTestClient.get()
            .uri("/api/auth/status")
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertFalse(res.authenticated)
        Assertions.assertFalse(res.stepUp)
        Assertions.assertNull(res.emailVerified)
        Assertions.assertFalse(res.twoFactorRequired)
        Assertions.assertNull(res.twoFactorMethods)
        Assertions.assertNull(res.preferredTwoFactorMethod)
    }
    @Test fun `status works authenticated`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/status")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.authenticated)
        Assertions.assertFalse(res.stepUp)
        Assertions.assertFalse(res.emailVerified!!)
        Assertions.assertFalse(res.twoFactorRequired)
        Assertions.assertNull(res.twoFactorMethods)
        Assertions.assertNull(res.preferredTwoFactorMethod)
    }
    @Test fun `status works authenticated and email verified`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.email.verified = true
        userService.save(user.info)

        val res = webTestClient.get()
            .uri("/api/auth/status")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.authenticated)
        Assertions.assertFalse(res.stepUp)
        Assertions.assertTrue(res.emailVerified!!)
        Assertions.assertFalse(res.twoFactorRequired)
        Assertions.assertNull(res.twoFactorMethods)
        Assertions.assertNull(res.preferredTwoFactorMethod)
    }
    @Test fun `status works with step up`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/status")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.authenticated)
        Assertions.assertTrue(res.stepUp)
        Assertions.assertFalse(res.emailVerified!!)
        Assertions.assertFalse(res.twoFactorRequired)
        Assertions.assertNull(res.twoFactorMethods)
        Assertions.assertNull(res.preferredTwoFactorMethod)
    }
    @Test fun `status works with step up but not authenticated`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/status")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertFalse(res.authenticated)
        Assertions.assertFalse(res.stepUp)
        Assertions.assertNull(res.emailVerified)
        Assertions.assertFalse(res.twoFactorRequired)
        Assertions.assertNull(res.twoFactorMethods)
        Assertions.assertNull(res.preferredTwoFactorMethod)
    }
    @Test fun `status works with two-factor`() = runTest {
        val user = registerUser(totpEnabled = true)

        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.id).getOrThrow()

        val res = webTestClient.get()
            .uri("/api/auth/status")
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertFalse(res.authenticated)
        Assertions.assertFalse(res.stepUp)
        Assertions.assertFalse(res.emailVerified!!)
        Assertions.assertTrue(res.twoFactorRequired)
        Assertions.assertEquals(res.twoFactorMethods , user.info.twoFactorMethods)
        Assertions.assertEquals(res.preferredTwoFactorMethod, user.info.preferredTwoFactorMethod.getOrThrow())
    }
    @Test fun `status works with two-factor and authenticated`() = runTest {
        val user = registerUser(totpEnabled = true)

        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.id).getOrThrow()

        val res = webTestClient.get()
            .uri("/api/auth/status")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(twoFactorToken.value)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.authenticated)
        Assertions.assertFalse(res.stepUp)
        Assertions.assertFalse(res.emailVerified!!)
        Assertions.assertTrue(res.twoFactorRequired)
        Assertions.assertEquals(res.twoFactorMethods , user.info.twoFactorMethods)
        Assertions.assertEquals(res.preferredTwoFactorMethod, user.info.preferredTwoFactorMethod.getOrThrow())
    }
}
