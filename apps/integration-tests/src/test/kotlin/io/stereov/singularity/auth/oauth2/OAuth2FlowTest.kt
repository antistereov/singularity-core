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
    @Test fun `register flow needs valid session token`() = runTest {
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
    @Test fun `register flow needs unexpired session token`() = runTest {
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
    @Test fun `login flow needs valid session token`() = runTest {
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
    @Test fun `login flow needs unexpired session token`() = runTest {
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
    // State
    @Test fun `connection flow needs state`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

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
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
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

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs valid state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val (_, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val redirectUri = "$loginPath?code=dummy-code&state=invalid"
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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs unexpired state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        mockOAuth2Server.enqueueResponses()

        runBlocking { delay(Duration.ofSeconds(3))}

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STATE_EXPIRED)

        mockOAuth2Server.verifyRequests()

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // SessionToken
    @Test fun `connection flow needs session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_MISSING)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs valid session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie("invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_SESSION_TOKEN)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs unexpired session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), Instant.ofEpochSecond(0))
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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_EXPIRED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // AccessToken
    @Test fun `connection flow needs access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.ACCESS_TOKEN_MISSING)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs valid access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie("invalid")
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_ACCESS_TOKEN)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs unexpired access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie(accessTokenService.create(registeredUser.info, registeredUser.sessionId, Instant.ofEpochSecond(0)))
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.ACCESS_TOKEN_EXPIRED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // StepUpToken
    @Test fun `connection flow needs stepUp token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STEP_UP_MISSING)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs valid stepUp token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .stepUpTokenCookie("invalid")
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_STEP_UP_TOKEN)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs unexpired stepUp token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .stepUpTokenCookie(stepUpTokenService.create(registeredUser.info.id, registeredUser.sessionId, Instant.ofEpochSecond(0)))
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STEP_UP_TOKEN_EXPIRED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // OAuth2ProviderConnectionToken
    @Test fun `connection flow needs connection token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie(registeredUser.accessToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs valid connection token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie("invalid")
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_CONNECTION_TOKEN)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs unexpired connection token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github", Instant.ofEpochSecond(0))

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.CONNECTION_TOKEN_EXPIRED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs connection token for same provider`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "not-github")

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.CONNECTION_TOKEN_PROVIDER_MISMATCH)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // Claims
    @Test fun `connection flow needs sub`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses(sub = null)

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SUB_CLAIM_MISSING)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs email`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses(email = null)

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.EMAIL_CLAIM_MISSING)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `connection flow needs username`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(provider = "another", principalId = principalId)

        mockOAuth2Server.enqueueResponses(login = null)

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        val user = userService.findByEmail(registeredUser.email!!)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }

    // STEP-UP

    @Test fun `stepUp flow works`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val info = mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))


        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
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

        assertEquals(1, user.sensitive.sessions.size)

        requireNotNull(res)
        val accessToken = res.extractAccessToken()
        val refreshToken = res.extractRefreshToken()
        val stepUpToken = res.extractStepUpToken(accessToken.userId, accessToken.sessionId)

        assertEquals(user.id, accessToken.userId)
        assertEquals(user.id, refreshToken.userId)
        assertEquals(user.id, stepUpToken.userId)
        assertEquals(refreshToken.sessionId, accessToken.sessionId)
        assertEquals(refreshToken.sessionId, stepUpToken.sessionId)
        assertEquals(setOf(refreshToken.sessionId), user.sensitive.sessions.keys)
    }
    // State
    @Test fun `stepUp flow needs state`() = runTest {
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
            .accessTokenCookie(registeredUser.accessToken)
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
    @Test fun `stepUp flow needs valid state token`() = runTest {
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
            .accessTokenCookie(registeredUser.accessToken)
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
    @Test fun `stepUp flow needs unexpired state token`() = runTest {
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
            .accessTokenCookie(registeredUser.accessToken)
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
    @Test fun `stepUp flow needs session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
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
    @Test fun `stepUp flow needs valid session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie("invalid")
            .accessTokenCookie(registeredUser.accessToken)
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
    @Test fun `stepUp flow needs unexpired session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), Instant.ofEpochSecond(0))

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
    // AccessToken
    @Test fun `stepUp flow needs access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.ACCESS_TOKEN_MISSING)

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertEquals(1, user.sensitive.sessions.size)

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
    @Test fun `stepUp flow needs valid access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie("invalid")
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_ACCESS_TOKEN)

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertEquals(1, user.sensitive.sessions.size)

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
    @Test fun `stepUp flow needs unexpired access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(accessTokenService.create(registeredUser.info, registeredUser.sessionId, Instant.ofEpochSecond(0)))
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.ACCESS_TOKEN_EXPIRED)

        val user = userService.findByEmail(registeredUser.email)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertEquals(1, user.sensitive.sessions.size)

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
    @Test fun `stepUp flow needs sub`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = null, email = registeredUser.email!!)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

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
    @Test fun `stepUp flow needs email`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        mockOAuth2Server.enqueueResponses(sub = principalId, email = null)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

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
    @Test fun `stepUp flow needs username`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val principalId = "12345"
        val registeredUser = registerOAuth2(principalId = principalId)
        registeredUser.info.clearSessions()
        userService.save(registeredUser.info)

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
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
            .accessTokenCookie(registeredUser.accessToken)
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
    // Authentication
    @Test fun `stepUp flow throws when already authenticated with password`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
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
            .assertErrorCode(OAuth2ErrorCode.WRONG_ACCOUNT_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertEquals(2, userService.findAll().toList().size)

        val user = userService.findByEmail(oauth.email)
        assertEquals(oauth.info.id, user.id)
        assertEquals(2, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertEquals(1, user.sensitive.sessions.size)

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
    @Test fun `stepUp flow throws when already authenticated with different oauth2`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
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
            .assertErrorCode(OAuth2ErrorCode.WRONG_ACCOUNT_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertEquals(2, userService.findAll().toList().size)

        val user = userService.findByEmail(oauth.email)
        assertEquals(oauth.info.id, user.id)
        assertEquals(2, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertEquals(1, user.sensitive.sessions.size)

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
    @Test fun `stepUp flow throws when already authenticated with different oauth2 provider`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri&step_up=true")
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
            .assertErrorCode(OAuth2ErrorCode.WRONG_ACCOUNT_AUTHENTICATED)

        mockOAuth2Server.verifyRequests()

        assertEquals(2, userService.findAll().toList().size)

        val user = userService.findByEmail(oauth.email)
        assertEquals(oauth.info.id, user.id)
        assertEquals(2, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        assertEquals(1, user.sensitive.sessions.size)

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

    // CONVERTING GUESTS TO USER ACCOUNTS

    @Test fun `concerting guest to user flow works`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

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

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(info.email!!, user.sensitive.email!!)
        assertEquals(mutableSetOf(Role.USER), user.roles)

        val identities = user.sensitive.identities
        assertEquals(1, identities.size)

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
    // State
    @Test fun `concerting guest to user flow needs state`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

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
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs valid state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val (_, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        val redirectUri = "$loginPath?code=dummy-code&state=invalid"
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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs unexpired state token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        mockOAuth2Server.enqueueResponses()

        runBlocking { delay(Duration.ofSeconds(3))}

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STATE_EXPIRED)

        mockOAuth2Server.verifyRequests()

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // SessionToken
    @Test fun `concerting guest to user flow needs session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_MISSING)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs valid session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github")

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie("invalid")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_SESSION_TOKEN)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs unexpired session token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), Instant.ofEpochSecond(0))
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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SESSION_TOKEN_EXPIRED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // AccessToken
    @Test fun `concerting guest to user flow needs access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.ACCESS_TOKEN_MISSING)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs valid access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie("invalid")
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_ACCESS_TOKEN)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs unexpired access token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie(accessTokenService.create(registeredUser.info, registeredUser.sessionId, Instant.ofEpochSecond(0)))
            .oauth2ConnectionCookie(providerConnectionToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.ACCESS_TOKEN_EXPIRED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // StepUpToken
    @Test fun `concerting guest to user flow needs stepUp token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STEP_UP_MISSING)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs valid stepUp token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .stepUpTokenCookie("invalid")
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_STEP_UP_TOKEN)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs unexpired stepUp token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .stepUpTokenCookie(stepUpTokenService.create(registeredUser.info.id, registeredUser.sessionId, Instant.ofEpochSecond(0)))
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.STEP_UP_TOKEN_EXPIRED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // OAuth2ProviderConnectionToken
    @Test fun `concerting guest to user flow needs connection token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie(registeredUser.accessToken)
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs valid connection token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

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
            .accessTokenCookie(registeredUser.accessToken)
            .oauth2ConnectionCookie("invalid")
            .stepUpTokenCookie(registeredUser.stepUpToken)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        mockOAuth2Server.verifyRequests()

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.INVALID_CONNECTION_TOKEN)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs unexpired connection token`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "github", Instant.ofEpochSecond(0))

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.CONNECTION_TOKEN_EXPIRED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs connection token for same provider`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses()

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id, registeredUser.sessionId, "not-github")

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.CONNECTION_TOKEN_PROVIDER_MISMATCH)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    // Claims
    @Test fun `concerting guest to user flow needs sub`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses(sub = null)

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.SUB_CLAIM_MISSING)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs email`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses(email = null)

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.EMAIL_CLAIM_MISSING)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }
    @Test fun `concerting guest to user flow needs username`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        val registeredUser = createGuest()

        mockOAuth2Server.enqueueResponses(login = null)

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

        res.responseHeaders
            .location
            .assertErrorCode(OAuth2ErrorCode.AUTHENTICATION_FAILED)

        val user = userService.findById(registeredUser.info.id)
        assertEquals(registeredUser.info.id, user.id)
        assertEquals(1, userService.findAll().toList().size)
        assertEquals(mutableSetOf(Role.GUEST), user.roles)
        assertNull(user.sensitive.email)

        val identities = user.sensitive.identities
        assertEquals(0, identities.size)

        assertNull(identities["github"])

        requireNotNull(res)
        assertThrows<TokenException> { res.extractAccessToken() }
        assertThrows<TokenException> { res.extractRefreshToken() }
        assertThrows<TokenException> { res.extractStepUpToken(registeredUser.info.id, registeredUser.sessionId) }
    }

}