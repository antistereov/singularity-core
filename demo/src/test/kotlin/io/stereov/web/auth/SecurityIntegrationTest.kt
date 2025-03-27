package io.stereov.web.auth

import io.stereov.web.BaseIntegrationTest
import io.stereov.web.config.Constants
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SecurityIntegrationTest : BaseIntegrationTest() {

    @Test fun `access with valid token`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `valid token required`() = runTest {
        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, "access_token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `token and user account required`() = runTest {
        val user = registerUser()
        deleteAccount(user)

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `Unexpired token required`() = runTest {
        val user = registerUser()
        val token = userTokenService.createAccessToken(user.info.id!!, "device", 2)

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, token)
            .exchange()
            .expectStatus().isOk

        Thread.sleep(2000)

        webTestClient.get()
            .uri("/user/me")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, token)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
