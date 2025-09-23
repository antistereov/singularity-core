package io.stereov.singularity.auth.oauth2

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.test.BaseOAuth2FlowTest
import io.stereov.singularity.user.core.exception.model.UserDoesNotExistException
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.delay
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant

class OAuth2FlowTest() : BaseOAuth2FlowTest() {

    // REGISTER

    @Test fun `register flow works`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val info = mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(info.email!!)
        assertEquals(info.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        val session = user.sensitive.sessions.values.first()
        assertEquals(sessionToken.browser, session.browser)
        assertEquals(sessionToken.os, session.os)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    @Test fun `register flow works with session token via request param`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"
        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()


        val info = mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(info.email!!)
        assertEquals(info.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        val session = user.sensitive.sessions.values.first()
        assertEquals(sessionToken.browser, session.browser)
        assertEquals(sessionToken.os, session.os)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    @Test fun `register flow session token via request param overrides cookie`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val info = mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie("invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res?.toString())

        val user = userService.findByEmail(info.email!!)
        assertEquals(info.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)
    }
    // State
    @Test fun `register flow needs state`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (_, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))


        val redirectUri = "$loginPath?code=dummy-code"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)


        assertTrue(userService.findAll().toList().isEmpty())
    }
    @Test fun `register flow needs valid state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val redirectUri = "$loginPath?code=dummy-code&state=${state}1"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        assertTrue(userService.findAll().toList().isEmpty())
    }
    @Test fun `register flow needs unexpired state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val info = mockOAuth2Server.enqueueResponses()

        runBlocking { delay(Duration.ofSeconds(3)) }

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STATE_EXPIRED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    // Session
    @Test fun `register flow needs session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_MISSING)

        mockOAuth2Server.verifyRequests()

        assertTrue(userService.findAll().toList().isEmpty())
    }
    @Test fun `register flow needs valid session token via cookie`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie("invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_SESSION_TOKEN)

        mockOAuth2Server.verifyRequests()

        assertTrue(userService.findAll().toList().isEmpty())
    }
    @Test fun `register flow needs unexpired session token via cookie`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), Instant.ofEpochSecond(0))

        val info = mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_EXPIRED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    @Test fun `register flow needs valid session token via request param`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_SESSION_TOKEN)

        mockOAuth2Server.verifyRequests()

        assertTrue(userService.findAll().toList().isEmpty())
    }
    @Test fun `register flow needs unexpired session token via request param`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), Instant.ofEpochSecond(0))

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val info = mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_EXPIRED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    // Claims
    @Test fun `register flow needs sub`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val info = mockOAuth2Server.enqueueResponses(sub = null)

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SUB_CLAIM_MISSING)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    @Test fun `register flow needs email`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        mockOAuth2Server.enqueueResponses(email = null)

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.EMAIL_CLAIM_MISSING)

        mockOAuth2Server.verifyRequests()

        assertTrue(userService.findAll().toList().isEmpty())
    }
    @Test fun `register flow needs username`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val info = mockOAuth2Server.enqueueResponses(login = null)

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    // Register
    @Test fun `register flow needs unregistered email`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerUser()
        val info = mockOAuth2Server.enqueueResponses(email = registeredUser.email)

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.EMAIL_ALREADY_REGISTERED)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(info.email!!)
        assertEquals(info.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])
        assertTrue(hashService.checkBcrypt(registeredUser.password!!, user.password!!))
    }
    @Test fun `register flow throws when already authenticated with password`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerUser()
        val info = mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    @Test fun `register flow throws when already authenticated with different oauth2`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerOAuth2()
        val info = mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .accessTokenCookie(registeredUser.accessToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    @Test fun `register flow throws when already authenticated with same oauth2`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerOAuth2()
        val info = mockOAuth2Server.enqueueResponses(sub = registeredUser.info.sensitive.identities["github"]!!.principalId)

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .accessTokenCookie(registeredUser.accessToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }
    @Test fun `register flow throws when already authenticated with different oauth2 provider`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerOAuth2(provider = "another")
        val info = mockOAuth2Server.enqueueResponses(sub = registeredUser.info.sensitive.identities["another"]!!.principalId)

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .accessTokenCookie(registeredUser.accessToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertThrows<UserDoesNotExistException> { userService.findByEmail(info.email!!) }
    }

    // LOGIN

    @Test fun `login flow works`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val info = mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))


        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(info.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(info.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        val session = user.sensitive.sessions.values.first()
        assertEquals(sessionToken.browser, session.browser)
        assertEquals(sessionToken.os, session.os)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        assertThrows<TokenException> { res.extractStepUpToken(accessToken.userId, accessToken.sessionId) }

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    @Test fun `login flow works with session token via request param`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"
        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val info = mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(info.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(info.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        val session = user.sensitive.sessions.values.first()
        assertEquals(sessionToken.browser, session.browser)
        assertEquals(sessionToken.os, session.os)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        assertThrows<TokenException> { res.extractStepUpToken(accessToken.userId, accessToken.sessionId) }

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    @Test fun `login flow session token via request param overrides cookie`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val info = mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie("invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(info.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(info.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        val session = user.sensitive.sessions.values.first()
        assertEquals(sessionToken.browser, session.browser)
        assertEquals(sessionToken.os, session.os)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        assertThrows<TokenException> { res.extractStepUpToken(accessToken.userId, accessToken.sessionId) }

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    // State
    @Test fun `login flow needs state`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val (_, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val redirectUri = "$loginPath?code=dummy-code"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        assertTrue(user.sensitive.sessions.isEmpty())

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs valid state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val redirectUri = "$loginPath?code=dummy-code&state=${state}1"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        assertTrue(user.sensitive.sessions.isEmpty())

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs unexpired state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))


        runBlocking { delay(Duration.ofSeconds(3)) }

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STATE_EXPIRED)

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        assertTrue(user.sensitive.sessions.isEmpty())

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // Session
    @Test fun `login flow needs session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_MISSING)

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertTrue(user.sensitive.sessions.isEmpty())

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs valid session token via cookie`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie("invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_SESSION_TOKEN)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertTrue(user.sensitive.sessions.isEmpty())

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs unexpired session token via cookie`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), Instant.ofEpochSecond(0))

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_EXPIRED)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertTrue(user.sensitive.sessions.isEmpty())

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs valid session token via request param`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_SESSION_TOKEN)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        assertTrue(user.sensitive.sessions.isEmpty())

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs unexpired session token via request param`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), Instant.ofEpochSecond(0))

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_EXPIRED)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertTrue(user.sensitive.sessions.isEmpty())

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // Claims
    @Test fun `login flow needs sub`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = null, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SUB_CLAIM_MISSING)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertTrue(user.sensitive.sessions.isEmpty())

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs email`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = null)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.EMAIL_CLAIM_MISSING)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertTrue(user.sensitive.sessions.isEmpty())

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `login flow needs username`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!, login = null)

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertTrue(user.sensitive.sessions.isEmpty())

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, registeredUser.info.sensitive.identities.values.first().principalId)
        assertEquals(1, registeredUser.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // Login
    @Test fun `login flow throws when already authenticated with password`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerUser()
        val oauth = registerOAuth2(principalId = "12345")
        oauth.info.clearSessions()
        userService.save(oauth.info)

        mockOAuth2Server.enqueueResponses(email = oauth.email!!, sub = "12345")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertEquals(2, userService.findAll().toList().size)

        val user = userService.findByEmail(oauth.email)
        assertEquals(oauth.info.id, user.id)
        assertEquals(2, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertTrue(user.sensitive.sessions.isEmpty())

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, oauth.info.sensitive.identities.values.first().principalId)
        assertEquals(1, oauth.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(oauth.info.id, oauth.sessionId) }
    }
    @Test fun `login flow throws when already authenticated with different oauth2`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerOAuth2()
        val oauth = registerOAuth2(principalId = "12345")
        oauth.info.clearSessions()
        userService.save(oauth.info)
        mockOAuth2Server.enqueueResponses(email = oauth.email!!, sub = "12345")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .accessTokenCookie(registeredUser.accessToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertEquals(2, userService.findAll().toList().size)

        val user = userService.findByEmail(oauth.email)
        assertEquals(oauth.info.id, user.id)
        assertEquals(2, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertTrue(user.sensitive.sessions.isEmpty())

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, oauth.info.sensitive.identities.values.first().principalId)
        assertEquals(1, oauth.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(oauth.info.id, oauth.sessionId) }
    }
    @Test fun `login flow throws when already authenticated with different oauth2 provider`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val registeredUser = registerOAuth2(provider = "another")
        val oauth = registerOAuth2(principalId = "12345")
        oauth.info.clearSessions()
        userService.save(oauth.info)
        mockOAuth2Server.enqueueResponses(email = oauth.email!!, sub = "12345")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .accessTokenCookie(registeredUser.accessToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertEquals(2, userService.findAll().toList().size)

        val user = userService.findByEmail(oauth.email)
        assertEquals(oauth.info.id, user.id)
        assertEquals(2, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertTrue(user.sensitive.sessions.isEmpty())

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(githubIdentity.principalId, oauth.info.sensitive.identities.values.first().principalId)
        assertEquals(1, oauth.info.sensitive.identities.size)

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(oauth.info.id, oauth.sessionId) }
    }

    // CONNECTION

    @Test fun `connection flow works`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        val info = mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(registeredUser.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(2, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        assertEquals(1, user.sensitive.sessions.size)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        assertThrows<TokenException> { res.extractStepUpToken(accessToken.userId, accessToken.sessionId) }

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    @Test fun `connection flow works with query params`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val info = mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}&oauth2_provider_connection_token=${providerConnectionToken.value}&step_up_token=${registeredUser.stepUpToken}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(registeredUser.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(2, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        assertEquals(1, user.sensitive.sessions.size)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        assertThrows<TokenException> { res.extractStepUpToken(accessToken.userId, accessToken.sessionId) }

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    @Test fun `connection flow query params override cookies`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val info = mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&session_token=${sessionToken.value}&oauth2_provider_connection_token=${providerConnectionToken.value}&step_up_token=${registeredUser.stepUpToken}")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie("invalid")
            .stepUpTokenCookie("invalid")
            .sessionTokenCookie("invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        assertEquals(successRedirectUri, res.responseHeaders.location?.toString())

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(registeredUser.email, user.sensitive.email)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(2, identities.size)

        val githubIdentity = requireNotNull(identities["github"])
        assertEquals(info.sub, githubIdentity.principalId)

        assertEquals(1, user.sensitive.sessions.size)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        assertThrows<TokenException> { res.extractStepUpToken(accessToken.userId, accessToken.sessionId) }

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }

    // STEP-UP

    // CONVERTING GUESTS TO USER ACCOUNTS

}