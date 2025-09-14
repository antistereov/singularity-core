package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.AuthenticationStatusResponse
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.response.StepUpResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class TwoFactorAuthControllerTest : BaseIntegrationTest() {

    @Test fun `verifyLogin works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret)

        val res = webTestClient.post()
            .uri("/api/auth/2fa/login?code=$code")
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
    @Test fun `verifyLogin needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/login?code=$code")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyLogin needs 2fa token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/2fa/login?code=25234")
            .bodyValue(SessionInfoRequest("session"))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyLogin needs body`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/api/auth/2fa/login")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `loginStatus works is not pending if no cookie is set`() = runTest {
        val status0 = webTestClient.get()
            .uri("/api/auth/2fa/login/status")
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        Assertions.assertFalse(status0?.twoFactorRequired!!)
    }
    @Test fun `loginStatus not pending with invalid cookie`() = runTest {
        val status1 = webTestClient.get()
            .uri("/api/auth/2fa/login/status")
            .cookie(TwoFactorTokenType.Authentication.cookieName, "test")
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        Assertions.assertFalse(status1?.twoFactorRequired!!)
    }
    @Test fun `loginStatus works when valid cookie is set`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        requireNotNull(user.twoFactorToken)

        val res = webTestClient.get()
            .uri("/api/auth/2fa/login/status")
            .cookie(TwoFactorTokenType.Authentication.cookieName, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        Assertions.assertTrue(res?.twoFactorRequired!!)
    }

    @Test fun `verifyStepUp works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
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
    @Test fun `verifyStepUp requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.totpSecret)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code=$twoFactorCode")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp requires 2fa code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyStepUp requires valid 2fa code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val twoFactorCode = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyStepUp requires enabled 2fa`() = runTest {
        val user = registerUser()
        val twoFactorCode = gAuth.getTotpPassword(user.totpSecret) + 1

        webTestClient.post()
            .uri("/api/auth/2fa/step-up?code=$twoFactorCode")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `stepUpStatus works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                cookieCreator.createCookie(
                    stepUpTokenService.create(
                        user.info.id,
                        user.sessionId
                    )
                ).value
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertFalse(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires token for same user`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        val anotherUser = registerUser("another@email.com")

        val res = webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                cookieCreator.createCookie(
                    stepUpTokenService.create(
                        anotherUser.info.id,
                        user.sessionId
                    )
                ).value
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires token for same session`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                cookieCreator.createCookie(stepUpTokenService.create(user.info.id, UUID.randomUUID())).value
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires valid token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, "another-token")
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires unexpired token`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        val res = webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                cookieCreator.createCookie(
                    stepUpTokenService.create(
                        user.info.id,
                        user.sessionId,
                        Instant.ofEpochSecond(0)
                    )
                ).value
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus requires authentication`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(
                SessionTokenType.StepUp.cookieName,
                cookieCreator.createCookie(
                    stepUpTokenService.create(
                        user.info.id,
                        user.sessionId
                    )
                ).value
            )
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `stepUpStatus 2fa is not required for disabled 2fa when cookie is set`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                cookieCreator.createCookie(
                    stepUpTokenService.create(
                        user.info.id,
                        user.sessionId
                    )
                ).value
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertFalse(res.twoFactorRequired)
    }
    @Test fun `stepUpStatus is not required for disabled 2fa when cookie is not set`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/2fa/step-up/status")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthenticationStatusResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertFalse(res.twoFactorRequired)
    }


}
