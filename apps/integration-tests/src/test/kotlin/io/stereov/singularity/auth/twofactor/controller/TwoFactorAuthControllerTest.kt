package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.request.CompleteLoginRequest
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.dto.response.StepUpResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

class TwoFactorAuthControllerTest : BaseIntegrationTest() {

    @Test fun `verifyLogin with TOTP works`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        val res = webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = code))
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        Assertions.assertEquals(user.info.id, body.user.id)
        val token = res.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.first()
            ?.value
        Assertions.assertEquals("", token)

        Assertions.assertFalse(res.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
        Assertions.assertFalse(res.responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value.isNullOrEmpty())
    }
    @Test fun `verifyLogin with TOTP needs correct code`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteStepUpRequest(totp = code))
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with TOTP needs valid 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with TOTP needs unexpired 2fa token`() = runTest {
        val user = registerUser(totpEnabled = true)
        val twoFactorToken = twoFactorAuthenticationTokenService.create(user.info.id, Instant.ofEpochSecond(0))

        val code = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .cookie(TwoFactorTokenType.Authentication.cookieName, twoFactorToken.value)
            .bodyValue(CompleteLoginRequest(totp = code))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin with TOTP needs body`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin with TOTP needs TOTP code`() = runTest {
        val user = registerUser(totpEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .bodyValue(CompleteLoginRequest(email = "123456"))
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }


    @Test fun `verifyStepUp with TOTP works`() = runTest {
        val user = registerUser(totpEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.totpSecret)

        val res = webTestClient.post()
            .uri("/api/auth/2fa/step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(StepUpResponse::class.java)
            .returnResult()

        val token = res.responseCookies[SessionTokenType.StepUp.cookieName]?.first()?.value

        requireNotNull(token)
    }
    @Test fun `verifyStepUp with TOTP requires authentication`() = runTest {
        val user = registerUser(totpEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code=$twoFactorCode")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP requires 2fa code`() = runTest {
        val user = registerUser(totpEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp with TOTP requires valid 2fa code`() = runTest {
        val user = registerUser(totpEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp with TOTP requires enabled 2fa`() = runTest {
        val user = registerUser()
        val twoFactorCode = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
}
