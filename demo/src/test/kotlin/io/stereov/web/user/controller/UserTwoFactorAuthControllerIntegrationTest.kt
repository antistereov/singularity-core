package io.stereov.web.user.controller

import io.stereov.web.test.BaseIntegrationTest
import io.stereov.web.config.Constants
import io.stereov.web.user.dto.*
import io.stereov.web.user.dto.request.DeviceInfoRequest
import io.stereov.web.user.dto.request.LoginRequest
import io.stereov.web.user.dto.response.LoginResponse
import io.stereov.web.user.dto.response.TwoFactorSetupResponse
import io.stereov.web.user.dto.response.TwoFactorStatusResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserTwoFactorAuthControllerIntegrationTest : BaseIntegrationTest() {

    @Test fun `2fa setup succeeds`() = runTest {
        val email = "test@email.com"
        val password = "password"
        val deviceId = "device"
        val user = registerUser(email, password, deviceId)
        val login = LoginRequest(email, password, DeviceInfoRequest(deviceId))

        val response = webTestClient.post()
            .uri("/user/2fa/setup")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorSetupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response)

        val code = gAuth.getTotpPassword(response.secret)

        val loginRes = webTestClient.post()
            .uri("/user/login")
            .bodyValue(login)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()

        val loginResBody = loginRes.responseBody
        requireNotNull(loginResBody)

        val twoFactorToken = loginRes.responseCookies[Constants.TWO_FACTOR_AUTH_COOKIE]
            ?.first()
            ?.value

        requireNotNull(twoFactorToken)

        assertTrue(loginResBody.twoFactorRequired)
        assertEquals(user.info.id, loginResBody.user.id)

        val userRes = webTestClient.post()
            .uri("/user/2fa/verify?code=$code")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(userRes)

        assertTrue(userRes.twoFactorAuthEnabled)
        assertEquals(user.info.idX, userRes.id)
    }
    @Test fun `2fa setup requires authentication`() = runTest {
        registerUser()
        webTestClient.post()
            .uri("/user/2fa/setup")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `2fa verification works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.twoFactorSecret)

        val res = webTestClient.post()
            .uri("/user/2fa/verify?code=$code")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.toDto(), body)
        val token = res.responseCookies[Constants.TWO_FACTOR_AUTH_COOKIE]
            ?.first()
            ?.value
        assertEquals("", token)
    }
    @Test fun `2fa verification needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.info.security.twoFactor.secret) + 1

        webTestClient.post()
            .uri("/user/2fa/verify?code=$code")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa verification needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/user/2fa/verify")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `2fa verification needs 2fa token`() = runTest {
        webTestClient.post()
            .uri("/user/2fa/verify?code=25234")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `2fa recovery works`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)
        requireNotNull(user.twoFactorRecovery)

        val res = webTestClient.post()
            .uri("/user/2fa/recovery?code=${user.twoFactorRecovery}")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserDto::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.info.id, body.id)
        val token = res.responseCookies[Constants.TWO_FACTOR_AUTH_COOKIE]
            ?.first()
            ?.value
        assertEquals("", token)
    }
    @Test fun `2fa recovery needs correct code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        val code = gAuth.getTotpPassword(user.info.security.twoFactor.secret) + 1

        webTestClient.post()
            .uri("/user/2fa/recovery?code=$code")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, user.twoFactorToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `2fa recovery needs param code`() = runTest {
        val user = registerUser(twoFactorEnabled = true)
        requireNotNull(user.twoFactorToken)

        webTestClient.post()
            .uri("/user/2fa/recovery")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, user.twoFactorToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `2fa recovery needs 2fa token`() = runTest {
        webTestClient.post()
            .uri("/user/2fa/recovery?code=25234")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `2fa status works is not pending if no cookie is set`() = runTest {
        val status0 = webTestClient.get()
            .uri("/user/2fa/status")
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        assertFalse(status0?.twoFactorRequired!!)
    }
    @Test fun `2fa status not pending with invalid cookie`() = runTest {
        val status1 = webTestClient.get()
            .uri("/user/2fa/status")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, "test")
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        assertFalse(status1?.twoFactorRequired!!)
    }
    @Test fun `2fa works when valid cookie is set`() = runTest {
        val user = registerUser(twoFactorEnabled = true)

        requireNotNull(user.twoFactorToken)

        val res = webTestClient.get()
            .uri("/user/2fa/status")
            .cookie(Constants.TWO_FACTOR_AUTH_COOKIE, user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(TwoFactorStatusResponse::class.java)
            .returnResult()
            .responseBody

        assertTrue(res?.twoFactorRequired!!)
    }
}
