package io.stereov.singularity.auth.oauth2

import com.github.michaelbull.result.getOrThrow
import io.mockk.clearMocks
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.alert.service.LoginAlertService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.identity.UserIdentity
import io.stereov.singularity.test.BaseOAuth2FlowTest
import io.stereov.singularity.test.config.MockSecurityAlertConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.*

@Import(MockSecurityAlertConfig::class)
class OAuth2AlertUnitTest : BaseOAuth2FlowTest() {

    @Test fun `login works with locale and session`() = runTest {
        val loginUserSlot = slot<User>()
        val loginLocaleSlot = slot<Locale?>()
        val loginSessionSlot = slot<SessionInfo>()

        coJustRun { loginAlertService.send(
            capture(loginUserSlot),
            captureNullable(loginLocaleSlot),
            capture(loginSessionSlot)
        ) }
        coJustRun { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable()) }

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

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), locale = Locale.ENGLISH).getOrThrow()

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

        val updatedUser = userService.findById(registeredUser.info.id.getOrThrow()).getOrThrow()

        coVerify(exactly = 0) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable())}
        coVerify(exactly = 1) { loginAlertService.send(any(), any(), any()) }
        assert(loginUserSlot.isCaptured)
        assertEquals(registeredUser.info.id, loginUserSlot.captured.id)
        assertEquals(Locale.ENGLISH, loginLocaleSlot.captured)
        assertEquals(updatedUser.sensitive.sessions.values.first(), loginSessionSlot.captured)
        assertEquals("browser", loginSessionSlot.captured.browser)
        assertEquals("os", loginSessionSlot.captured.os)
    }
    @Test fun `login works without locale and session`() = runTest {
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

        val sessionToken = sessionTokenService.create(SessionInfoRequest()).getOrThrow()

        val loginUserSlot = slot<User>()
        val loginLocaleSlot = slot<Locale?>()
        val loginSessionSlot = slot<SessionInfo>()

        coJustRun { loginAlertService.send(
            capture(loginUserSlot),
            captureNullable(loginLocaleSlot),
            capture(loginSessionSlot)
        ) }
        coJustRun { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable()) }

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

        coVerify(exactly = 0) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable())}
        coVerify(exactly = 1) { loginAlertService.send(any(), anyNullable(), anyNullable()) }
        assert(loginUserSlot.isCaptured)
        assertEquals(registeredUser.info.id, loginUserSlot.captured.id)
        assert(loginLocaleSlot.isNull)
    }

    @Test fun `connect works without locale`() = runTest {
        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) }
        coJustRun { loginAlertService.send(any(), anyNullable(), any()) }

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

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os")).getOrThrow()
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id.getOrThrow(), registeredUser.sessionId, "github")
            .getOrThrow()
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

        coVerify(exactly = 0) { loginAlertService.send(any(), anyNullable(), any()) }
        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(registeredUser.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.OAUTH_CONNECTED, alertTypeSlot.captured)
        assertEquals("github", providerKeySlot.captured)
    }
    @Test fun `connect works with locale`() = runTest {
        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) }
        coJustRun { loginAlertService.send(any(), anyNullable(), any()) }

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

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os")).getOrThrow()
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id.getOrThrow(), registeredUser.sessionId, "github").getOrThrow()

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

        coVerify(exactly = 0) { loginAlertService.send(any(), anyNullable(), any()) }
        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(registeredUser.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
    }

    @Test fun `register does not send`() = runTest {
        coJustRun { loginAlertService.send(any(), anyNullable(), any()) }
        coJustRun { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable()) }

        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os")).getOrThrow()

        mockOAuth2Server.enqueueResponses()

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

        coVerify(exactly = 0) { loginAlertService.send(any(), anyNullable(), any()) }
        coVerify(exactly = 0) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable())}
    }
    @Test fun `guest conversion does not send`() = runTest {
        coJustRun { loginAlertService.send(any(), anyNullable(), any()) }
        coJustRun { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable()) }

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

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os")).getOrThrow()
        val providerConnectionToken = oAuth2ProviderConnectionTokenService
            .create(registeredUser.info.id.getOrThrow(), registeredUser.sessionId, "github").getOrThrow()

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

        coVerify(exactly = 0) { loginAlertService.send(any(), anyNullable(), any()) }
        coVerify(exactly = 0) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable())}
    }
    @Test fun `stepUp does not send`() = runTest {
        coJustRun { loginAlertService.send(any(), anyNullable(), any()) }
        coJustRun { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable()) }

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

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os")).getOrThrow()


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

        coVerify(exactly = 0) { loginAlertService.send(any(), anyNullable(), any()) }
        coVerify(exactly = 0) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), anyNullable())}
    }

    @Test fun `disconnect works without locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.identities.providers["github"] = UserIdentity.ofProvider(principalId = "1234")
        userService.save(user.info)

        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) }

        webTestClient.delete()
            .uri("/api/users/me/providers/github")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
        assertEquals(SecurityAlertType.OAUTH_DISCONNECTED, alertTypeSlot.captured)
        assertEquals("github", providerKeySlot.captured)
    }
    @Test fun `disconnect totp works with locale`() = runTest {
        val user = registerUser()
        user.info.sensitive.identities.providers["github"] = UserIdentity.ofProvider(principalId = "1234")
        userService.save(user.info)

        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()
        val alertTypeSlot = slot<SecurityAlertType>()
        val providerKeySlot = slot<String?>()
        val twoFactorMethodSlot = slot<TwoFactorMethod?>()

        coJustRun { securityAlertService.send(
            capture(userSlot),
            captureNullable(localeSlot),
            capture(alertTypeSlot),
            captureNullable(providerKeySlot),
            captureNullable(twoFactorMethodSlot),
        ) }

        webTestClient.delete()
            .uri("/api/users/me/providers/github?locale=en")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        coVerify(exactly = 1) { securityAlertService.send(any(), anyNullable(), any(), anyNullable(), any()) }
        assert(userSlot.isCaptured)
        assertEquals(user.info.id, userSlot.captured.id)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
        assertEquals(SecurityAlertType.OAUTH_DISCONNECTED, alertTypeSlot.captured)
        assertEquals("github", providerKeySlot.captured)
    }

    @Autowired
    lateinit var loginAlertService: LoginAlertService
    @Autowired
    lateinit var securityAlertService: SecurityAlertService

    @BeforeEach
    fun setupAlertMocks() {
        clearMocks(loginAlertService)
        clearMocks(securityAlertService)
    }

    companion object {

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.email.enable") { true }
        }
    }
}
