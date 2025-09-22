package io.stereov.singularity.auth.oauth2

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriComponentsBuilder

class OAuth2FlowTest() : BaseIntegrationTest() {

    @Test fun `register flow works`() = runTest {
        val successRedirectUri = "http://localhost:8000/dashboard"

        mockOAuth2Server.enqueue(
            MockResponse()
                .setBody("{\"access_token\":\"mock-access-token\",\"token_type\":\"bearer\"}")
                .addHeader("Content-Type", "application/json")
        )
        mockOAuth2Server.enqueue(
            MockResponse()
                .setBody("{\"id\":\"123\",\"login\":\"testuser\",\"email\":\"test@example.com\"}")
                .addHeader("Content-Type", "application/json")
        )

        val result = webTestClient.get().uri("/oauth2/authorization/github?redirect_uri=$successRedirectUri")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        val state = UriComponentsBuilder.fromUri(result.responseHeaders.location!!)
            .build()
            .queryParams["state"]?.first()!!

        val sessionCookie = result.responseHeaders.getFirst("Set-Cookie")?.substringBefore(";")?.substringAfter("=")
        val session = SessionInfoRequest("browser", "os")
        val sessionToken = sessionTokenService.create(session)

        val redirectUri = "/login/oauth2/code/github?code=dummy-code&state=$state"
        val res = webTestClient.get().uri(redirectUri)
            .cookie("SESSION", sessionCookie!!)
            .sessionTokenCookie(sessionToken)
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()
            .responseHeaders
            .location

        assertEquals(successRedirectUri, res?.toString())
    }

    companion object {
        lateinit var mockOAuth2Server: MockWebServer

        @BeforeAll
        @JvmStatic
        fun setUp() {
            mockOAuth2Server = MockWebServer()
            mockOAuth2Server.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            mockOAuth2Server.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            val mockServerUrl = mockOAuth2Server.url("/").toString()
            registry.add("spring.security.oauth2.client.registration.github.provider") { "mock-github" }
            registry.add("spring.security.oauth2.client.provider.mock-github.authorization-uri") { "${mockServerUrl}oauth/authorize" }
            registry.add("spring.security.oauth2.client.provider.mock-github.token-uri") { "${mockServerUrl}oauth/token" }
            registry.add("spring.security.oauth2.client.provider.mock-github.user-info-uri") { "${mockServerUrl}userinfo" }
            registry.add("spring.security.oauth2.client.provider.mock-github.user-name-attribute") { "login" }
        }
    }
}