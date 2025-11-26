package io.stereov.singularity.auth.oauth2

import io.mockk.clearMocks
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.alert.service.IdentityProviderInfoService
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.test.BaseOAuth2FlowTest
import io.stereov.singularity.test.config.MockSecurityAlertConfig
import io.stereov.singularity.user.core.model.User
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
class OAuth2IdentityProviderInfoUnitTest : BaseOAuth2FlowTest() {

    @Test fun `register works without locale`() = runTest {
        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

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
        mockOAuth2Server.enqueueResponses(email = registeredUser.email)

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

        coVerify(exactly = 1) { identityProviderInfoService.send(any(), anyNullable()) }
        assertEquals(registeredUser.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
    }
    @Test fun `register works with locale`() = runTest {
        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"), locale = Locale.ENGLISH)

        val registeredUser = registerUser()
        mockOAuth2Server.enqueueResponses(email = registeredUser.email)

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

        coVerify(exactly = 1) { identityProviderInfoService.send(any(), anyNullable()) }
        assertEquals(registeredUser.info.id, userSlot.captured.id)
        assertEquals(Locale.ENGLISH, localeSlot.captured)
    }
    @Test fun `register works with another oauth2`() = runTest {
        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

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
        mockOAuth2Server.enqueueResponses(email = registeredUser.email)

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

        coVerify(exactly = 1) { identityProviderInfoService.send(any(), anyNullable()) }
        assertEquals(registeredUser.info.id, userSlot.captured.id)
        assert(localeSlot.isNull)
    }
    @Test fun `register does not send when not registered`() = runTest {
        val userSlot = slot<User>()
        val localeSlot = slot<Locale?>()

        coJustRun { identityProviderInfoService.send(
            capture(userSlot),
            captureNullable(localeSlot),
        ) }

        val successRedirectUri = "http://localhost:8000/dashboard"

        val (state, sessionCookie) = webTestClient.get()
            .uri("$authorizationPath?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .extractStateAndSession()

        val sessionToken = sessionTokenService.create(SessionInfoRequest("browser", "os"))

        mockOAuth2Server.enqueueResponses()

        val redirectUri = "$loginPath?code=dummy-code&state=$state"
        webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound

        mockOAuth2Server.verifyRequests()

        coVerify(exactly = 0) { identityProviderInfoService.send(any(), anyNullable()) }
    }

    @Autowired
    lateinit var identityProviderInfoService: IdentityProviderInfoService

    @BeforeEach
    fun setupAlertMocks() {
        clearMocks(identityProviderInfoService)
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
