package io.stereov.singularity.auth.oauth2.controller

import io.stereov.singularity.auth.oauth2.dto.request.OAuth2ProviderConnectionRequest
import io.stereov.singularity.auth.oauth2.dto.response.OAuth2ProviderConnectionTokenResponse
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody

class OAuth2ProviderControllerTest : BaseIntegrationTest() {

    @Test fun `generateToken works`() = runTest {
        val user = registerUser()
        val req = OAuth2ProviderConnectionRequest("github")

        val res = webTestClient.post()
            .uri("/api/users/me/providers/oauth2/token")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody<OAuth2ProviderConnectionTokenResponse>()
            .returnResult()

        val token = res.extractOAuth2ProviderConnectionToken(user.info)

        Assertions.assertEquals(user.id, token.userId)
        Assertions.assertEquals(user.sessionId, token.sessionId)
        Assertions.assertEquals(req.provider, token.provider)
    }
    @Test fun `generateToken works with guest`() = runTest {
        val user = createGuest()
        val req = OAuth2ProviderConnectionRequest("github")

        val res = webTestClient.post()
            .uri("/api/users/me/providers/oauth2/token")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody<OAuth2ProviderConnectionTokenResponse>()
            .returnResult()

        val token = res.extractOAuth2ProviderConnectionToken(user.info)

        Assertions.assertEquals(user.id, token.userId)
        Assertions.assertEquals(user.sessionId, token.sessionId)
        Assertions.assertEquals(req.provider, token.provider)
    }
    @Test fun `generateToken works with oauth2`() = runTest {
        val user = registerOAuth2()
        val req = OAuth2ProviderConnectionRequest("github")

        val res = webTestClient.post()
            .uri("/api/users/me/providers/oauth2/token")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody<OAuth2ProviderConnectionTokenResponse>()
            .returnResult()

        val token = res.extractOAuth2ProviderConnectionToken(user.info)

        Assertions.assertEquals(user.id, token.userId)
        Assertions.assertEquals(user.sessionId, token.sessionId)
        Assertions.assertEquals(req.provider, token.provider)
    }
    @Test fun `generateToken requires access token`() = runTest {
        val user = registerUser()
        val req = OAuth2ProviderConnectionRequest("github")

        webTestClient.post()
            .uri("/api/users/me/providers/oauth2/token")
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `generateToken requires step-up token`() = runTest {
        val user = registerUser()
        val req = OAuth2ProviderConnectionRequest("github")

        webTestClient.post()
            .uri("/api/users/me/providers/oauth2/token")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `generateToken requires valid session`() = runTest {
        val user = registerOAuth2()
        val req = OAuth2ProviderConnectionRequest("github")

        userService.save(user.info.clearSessions() as User)

        webTestClient.post()
            .uri("/api/users/me/providers/oauth2/token")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
}