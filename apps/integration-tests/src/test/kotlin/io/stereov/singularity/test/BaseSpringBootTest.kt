package io.stereov.singularity.test

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.every
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.group.model.GroupDocument
import io.stereov.singularity.group.model.GroupTranslation
import io.stereov.singularity.group.service.GroupService
import io.stereov.singularity.test.config.MockConfig
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.user.dto.request.*
import io.stereov.singularity.user.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.user.model.Role
import io.stereov.singularity.user.model.UserDocument
import io.stereov.singularity.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MockConfig::class)
class BaseSpringBootTest {

    @Autowired
    lateinit var applicationContext: ApplicationContext


    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var gAuth: GoogleAuthenticator

    @BeforeEach
    fun setupMocks() {
        val secret = "secret-key"
        val code = 123456

        every { gAuth.createCredentials().key } returns "secret-key"
        every { gAuth.authorize(any(), any()) } answers {
            firstArg<String>() == secret && secondArg<Int>() == code
        }
        every { gAuth.getTotpPassword(secret) } returns code
    }

    data class TestRegisterResponse(
        val info: UserDocument,
        val accessToken: String,
        val refreshToken: String,
        val twoFactorToken: String?,
        val twoFactorSecret: String?,
        val twoFactorRecovery: String?,
        val twoFactorSetupToken: String?,
        val mailVerificationSecret: String,
        val passwordResetSecret: String,
    )

    suspend fun createGroup(key: String = "test-group"): GroupDocument {
        val group = GroupDocument(key = key, translations = mutableMapOf(Language.EN to GroupTranslation("Test")))
        return groupService.save(group)
    }

    suspend fun registerUser(
        email: String = "test@email.com",
        password: String = "password",
        deviceId: String = "device",
        twoFactorEnabled: Boolean = false,
        name: String = "Name",
        roles: List<Role> = listOf(Role.USER),
        groups: List<String> = listOf(),
    ): TestRegisterResponse {
        val device = DeviceInfoRequest(id = deviceId)

        var responseCookies = webTestClient.post()
            .uri("/api/user/register?send-email=false")
            .bodyValue(RegisterUserRequest(email = email, password = password, name = name, device = device))
            .exchange()
            .expectStatus().isOk
            .returnResult<Void>()
            .responseCookies

        var accessToken = responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
        var refreshToken = responseCookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token contained in response" }
        requireNotNull(refreshToken) { "No refresh token contained in response" }

        var twoFactorToken: String? = null
        var twoFactorRecovery: String? = null
        var twoFactorSecret: String? = null
        var twoFactorStartSetupToken: String? = null

        if (twoFactorEnabled) {
            val twoFactorSetupStartRes = webTestClient.post()
                .uri("/api/user/2fa/start-setup")
                .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
                .bodyValue(TwoFactorStartSetupRequest(password))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .returnResult()
                .responseCookies[Constants.TWO_FACTOR_SETUP_TOKEN_COOKIE]?.firstOrNull()?.value

            requireNotNull(twoFactorSetupStartRes)

            twoFactorStartSetupToken = twoFactorSetupStartRes



            val twoFactorRes = webTestClient.get()
                .uri("/api/user/2fa/setup")
                .cookie(Constants.TWO_FACTOR_SETUP_TOKEN_COOKIE, twoFactorStartSetupToken)
                .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
                .exchange()
                .expectStatus().isOk
                .expectBody(TwoFactorSetupResponse::class.java)
                .returnResult()
                .responseBody

            twoFactorRecovery = twoFactorRes?.recoveryCodes?.firstOrNull()
            twoFactorSecret = twoFactorRes?.secret
            val twoFactorVerifyToken = twoFactorRes?.token

            requireNotNull(twoFactorVerifyToken)

            val twoFactorSetupReq = TwoFactorVerifySetupRequest(
                twoFactorVerifyToken,
                gAuth.getTotpPassword(twoFactorSecret)
            )

            webTestClient.post()
                .uri("/api/user/2fa/setup")
                .cookie(Constants.ACCESS_TOKEN_COOKIE, accessToken)
                .bodyValue(twoFactorSetupReq)
                .exchange()
                .expectStatus().isOk

            twoFactorToken = webTestClient.post()
                .uri("/api/user/login")
                .bodyValue(LoginRequest(email, password, device))
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies[Constants.LOGIN_VERIFICATION_TOKEN_COOKIE]
                ?.firstOrNull()
                ?.value

            responseCookies = webTestClient.post()
                .uri("/api/user/2fa/verify-login?code=${gAuth.getTotpPassword(twoFactorSecret)}")
                .bodyValue(DeviceInfoRequest(deviceId))
                .cookie(Constants.LOGIN_VERIFICATION_TOKEN_COOKIE, twoFactorToken!!)
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies

            accessToken = responseCookies[Constants.ACCESS_TOKEN_COOKIE]?.firstOrNull()?.value
            refreshToken = responseCookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value

            requireNotNull(accessToken) { "No access token contained in response" }
            requireNotNull(refreshToken) { "No refresh token contained in response" }
        }


        var user = userService.findByEmail(email)

        if (roles != listOf(Role.USER)) {
            roles.forEach { role ->
                user.addRole(role)
            }
        }
        user.sensitive.groups.addAll(groups)
        user = userService.save(user)

        val mailVerificationToken = user.sensitive.security.mail.verificationSecret
        val passwordResetToken = user.sensitive.security.mail.passwordResetSecret

        return TestRegisterResponse(user, accessToken, refreshToken, twoFactorToken, twoFactorSecret, twoFactorRecovery, twoFactorStartSetupToken, mailVerificationToken, passwordResetToken)
    }

    suspend fun deleteAccount(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/api/user/me")
            .header(HttpHeaders.COOKIE, "${Constants.ACCESS_TOKEN_COOKIE}=${response.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
}
