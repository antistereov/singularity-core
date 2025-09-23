package io.stereov.singularity.test

import com.fasterxml.jackson.annotation.JsonInclude
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class BaseOAuth2FlowTest : BaseIntegrationTest() {

    val authorizationPath = "/oauth2/authorization/github"
    val loginPath = "/login/oauth2/code/github"

    fun URI?.assertErrorCode(code: OAuth2ErrorCode) {
        val expected = URI.create("${oAuth2Properties.errorRedirectUri}?code=$code")

        requireNotNull(this)

        if (code == OAuth2ErrorCode.AUTHENTICATION_FAILED) {
            assertEquals(expected.path, this.path)

            val queryParams = UriComponentsBuilder.fromUri(this).build().queryParams
            assertEquals(OAuth2ErrorCode.AUTHENTICATION_FAILED.value, queryParams["code"]?.first())
            assertNotNull(queryParams["details"])
        } else {
            assertEquals(expected, this)
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class OAuth2User(
        val sub: String? = "123",
        val login: String? = "testuser",
        val email: String? = "test@example.com"
    )

    fun MockWebServer.enqueueResponses(
        sub: String? = "123",
        login: String? = "testuser",
        email: String? = "test@example.com"
    ) : OAuth2User {
        val info = OAuth2User(sub, login, email)

        enqueue(
            MockResponse()
                .setBody("{\"access_token\":\"mock-access-token\",\"token_type\":\"bearer\"}")
                .addHeader("Content-Type", "application/json")
        )
        enqueue(
            MockResponse()
                .setBody(objectMapper.writeValueAsString(info))
                .addHeader("Content-Type", "application/json")
        )

        return info
    }

    fun MockWebServer.verifyRequests() {
        val tokenRequest = takeRequest()
        assertThat(tokenRequest.path).isEqualTo("/oauth/token")

        val userInfoRequest = takeRequest()
        assertThat(userInfoRequest.path).isEqualTo("/userinfo")
    }

    fun EntityExchangeResult<*>.extractStateAndSession(): Pair<String, String> {
        val state = UriComponentsBuilder.fromUri(this.responseHeaders.location!!)
            .build()
            .queryParams["state"]?.first()
        val session = responseHeaders.getFirst("Set-Cookie")?.substringBefore(";")?.substringAfter("=")

        return requireNotNull(state) to requireNotNull(session)
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
            registry.add("singularity.security.jwt.expires-in") { 3 }
        }
    }
}