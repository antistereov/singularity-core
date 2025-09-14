package io.stereov.singularity.test

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.every
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.service.token.*
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.auth.group.service.GroupService
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TotpService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.hash.service.HashService
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
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MockConfig::class)
class BaseSpringBootTest {

    @Autowired
    lateinit var cookieCreator: CookieCreator

    @Autowired
    lateinit var stepUpTokenService: StepUpTokenService 

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var groupService: GroupService

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var gAuth: GoogleAuthenticator

    @Autowired
    lateinit var accessTokenService: AccessTokenService

    @Autowired
    lateinit var emailVerificationTokenService: EmailVerificationTokenService

    @Autowired
    lateinit var passwordResetTokenService: PasswordResetTokenService

    @Autowired
    lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    lateinit var totpService: TotpService

    @Autowired
    lateinit var hashService: HashService

    @Autowired
    lateinit var encryptionSecretService: EncryptionSecretService

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
        val totpSecret: String?,
        val totpRecovery: String?,
        val mailVerificationSecret: String,
        val passwordResetSecret: String,
        val sessionId: UUID
    )

    suspend fun createGroup(key: String = "test-group"): GroupDocument {
        val group = GroupDocument(key = key, translations = mutableMapOf(Language.EN to GroupTranslation("Test")))
        return groupService.save(group)
    }

    suspend fun registerUser(
        email: String = "test@email.com",
        password: String = "password",
        twoFactorEnabled: Boolean = false,
        name: String = "Name",
        roles: List<Role> = listOf(Role.USER),
        groups: List<String> = listOf(),
    ): TestRegisterResponse {

        var responseCookies = webTestClient.post()
            .uri("/api/auth/register?send-email=false")
            .bodyValue(RegisterUserRequest(email = email, password = password, name = name, session = null))
            .exchange()
            .expectStatus().isOk
            .returnResult<Void>()
            .responseCookies

        var accessToken = responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
        var refreshToken = responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value

        requireNotNull(accessToken) { "No access token contained in response" }
        requireNotNull(refreshToken) { "No refresh token contained in response" }

        var twoFactorToken: String? = null
        var twoFactorRecovery: String? = null
        var twoFactorSecret: String? = null

        var user = userService.findByEmail(email)

        if (twoFactorEnabled) {
            val stepUpToken = stepUpTokenService.create(user.id, user.sensitive.sessions.keys.first())

            val twoFactorRes = webTestClient.get()
                .uri("/api/auth/2fa/totp/setup")
                .cookie(SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpToken).value)
                .cookie(SessionTokenType.Access.cookieName, accessToken)
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
                .uri("/api/auth/2fa/totp/setup")
                .cookie(SessionTokenType.Access.cookieName, accessToken)
                .bodyValue(twoFactorSetupReq)
                .exchange()
                .expectStatus().isOk

            twoFactorToken = webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(LoginRequest(email, password, null))
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies[TwoFactorTokenType.Authentication.cookieName]
                ?.firstOrNull()
                ?.value

            responseCookies = webTestClient.post()
                .uri("/api/auth/2fa/login?code=${gAuth.getTotpPassword(twoFactorSecret)}")
                .cookie(TwoFactorTokenType.Authentication.cookieName, twoFactorToken!!)
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies

            accessToken = responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
            refreshToken = responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value

            requireNotNull(accessToken) { "No access token contained in response" }
            requireNotNull(refreshToken) { "No refresh token contained in response" }
        }
        
        if (roles != listOf(Role.USER)) {
            roles.forEach { role ->
                user.addRole(role)
            }
        }
        user.sensitive.groups.addAll(groups)
        user = userService.save(user)

        val mailVerificationToken = user.sensitive.security.mail.verificationSecret
        val passwordResetToken = user.sensitive.security.password.resetSecret

        return TestRegisterResponse(
            user,
            accessToken,
            refreshToken,
            twoFactorToken,
            twoFactorSecret,
            twoFactorRecovery,
            mailVerificationToken,
            passwordResetToken,
            user.sensitive.sessions.keys.first()
        )
    }

    suspend fun deleteAccount(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/api/users/me")
            .header(HttpHeaders.COOKIE, "${SessionTokenType.Access.cookieName}=${response.accessToken}")
            .exchange()
            .expectStatus().isOk
    }
}
