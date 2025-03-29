package io.stereov.web.test

import io.stereov.web.config.Constants
import io.stereov.web.user.dto.DeviceInfoRequest
import io.stereov.web.user.dto.LoginRequest
import io.stereov.web.user.dto.RegisterUserRequest
import io.stereov.web.user.dto.TwoFactorSetupResponse
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseSpringBootTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var userService: UserService

    data class TestRegisterResponse(
        val info: UserDocument,
        val accessToken: String,
        val refreshToken: String,
        val twoFactorToken: String?,
        val twoFactorSecret: String?,
        val twoFactorRecovery: String?,
    )

    suspend fun registerUser(
        email: String = "test@email.com",
        password: String = "password",
        deviceId: String = "device",
        twoFactorEnabled: Boolean = false,
    ): TestRegisterResponse {
        val device = DeviceInfoRequest(id = deviceId)

        val responseCookies = webTestClient.post()
            .uri("/user/register")
            .bodyValue(RegisterUserRequest(email = email, password = password, device = device))
            .exchange()
            .expectStatus().isOk
            .returnResult<Void>()
            .responseCookies

        val accessToken = responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
        val refreshToken = responseCookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token contained in response" }
        requireNotNull(refreshToken) { "No refresh token contained in response" }

        var twoFactorToken: String? = null
        var twoFactorRecovery: String? = null
        var twoFactorSecret: String? = null

        if (twoFactorEnabled) {
            val twoFactorRes = webTestClient.post()
                .uri("/user/2fa/setup")
                .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
                .exchange()
                .expectStatus().isOk
                .expectBody(TwoFactorSetupResponse::class.java)
                .returnResult()
                .responseBody

            twoFactorRecovery = twoFactorRes?.recoveryCode
            twoFactorSecret = twoFactorRes?.secret

            twoFactorToken = webTestClient.post()
                .uri("/user/login")
                .bodyValue(LoginRequest(email, password, device))
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies[Constants.TWO_FACTOR_AUTH_COOKIE]
                ?.firstOrNull()
                ?.value
        }


        val user = userService.findByEmailOrNull(email)
        requireNotNull(user) { "User associated to $email not saved" }

        return TestRegisterResponse(user, accessToken, refreshToken, twoFactorToken, twoFactorSecret, twoFactorRecovery)
    }

    suspend fun deleteAccount(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/user/me")
            .header(HttpHeaders.COOKIE, "${Constants.ACCESS_TOKEN_COOKIE}=${response.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
}
