package io.stereov.singularity.test

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.every
import io.stereov.singularity.auth.device.dto.DeviceInfoRequest
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.auth.group.service.GroupService
import io.stereov.singularity.auth.session.dto.request.LoginRequest
import io.stereov.singularity.auth.session.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.session.model.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorSetupInitRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.test.config.MockConfig
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
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

    @AfterEach
    fun clearDatabase() = runBlocking {
        userService.deleteAll()
    }

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

        var accessToken = responseCookies[SessionTokenType.Access.cookieKey]?.firstOrNull()?.value
        var refreshToken = responseCookies[SessionTokenType.Refresh.cookieKey]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token contained in response" }
        requireNotNull(refreshToken) { "No refresh token contained in response" }

        var twoFactorToken: String? = null
        var twoFactorRecovery: String? = null
        var twoFactorSecret: String? = null
        var twoFactorStartSetupToken: String? = null

        if (twoFactorEnabled) {
            val twoFactorSetupStartRes = webTestClient.post()
                .uri("/api/user/2fa/start-setup")
                .cookie(SessionTokenType.Access.cookieKey, accessToken)
                .bodyValue(TwoFactorSetupInitRequest(password))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .returnResult()
                .responseCookies[TwoFactorTokenType.InitSetup.cookieKey]?.firstOrNull()?.value

            requireNotNull(twoFactorSetupStartRes)

            twoFactorStartSetupToken = twoFactorSetupStartRes

            val twoFactorRes = webTestClient.get()
                .uri("/api/user/2fa/setup")
                .cookie(TwoFactorTokenType.InitSetup.cookieKey, twoFactorStartSetupToken)
                .cookie(SessionTokenType.Access.cookieKey, accessToken)
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
                .cookie(SessionTokenType.Access.cookieKey, accessToken)
                .bodyValue(twoFactorSetupReq)
                .exchange()
                .expectStatus().isOk

            twoFactorToken = webTestClient.post()
                .uri("/api/user/login")
                .bodyValue(LoginRequest(email, password, device))
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies[TwoFactorTokenType.Login.cookieKey]
                ?.firstOrNull()
                ?.value

            responseCookies = webTestClient.post()
                .uri("/api/user/2fa/verify-login?code=${gAuth.getTotpPassword(twoFactorSecret)}")
                .bodyValue(DeviceInfoRequest(deviceId))
                .cookie(TwoFactorTokenType.Login.cookieKey, twoFactorToken!!)
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies

            accessToken = responseCookies[SessionTokenType.Access.cookieKey]?.firstOrNull()?.value
            refreshToken = responseCookies[SessionTokenType.Refresh.cookieKey]?.firstOrNull()?.value

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
            .header(HttpHeaders.COOKIE, "${SessionTokenType.Access.cookieKey}=${response.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
}
