package io.stereov.singularity.test

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.every
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.model.token.AccessToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.SessionService
import io.stereov.singularity.auth.core.service.token.*
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.auth.group.service.GroupService
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorAuthenticationRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorAuthenticationToken
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TotpService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.cache.service.CacheService
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
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.time.Duration
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MockConfig::class)
class BaseSpringBootTest {

    @Autowired
    lateinit var cacheService: CacheService

    @Autowired
    lateinit var sessionService: SessionService

    @Autowired
    lateinit var authorizationService: AuthorizationService

    @Autowired
    lateinit var twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService

    @Autowired
    lateinit var sessionTokenService: SessionTokenService

    @Autowired
    lateinit var cookieCreator: CookieCreator

    @Autowired
    lateinit var stepUpTokenService: StepUpTokenService 

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var groupService: GroupService

    lateinit var webTestClient: WebTestClient

    @LocalServerPort
    lateinit var port: String

    @BeforeEach
    fun setupWebTestClient() {
        webTestClient = WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:$port")
            .responseTimeout(Duration.ofSeconds(30))
            .build()
    }

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
        cacheService.deleteAll()
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
        val email: String,
        val password: String,
        val accessToken: String,
        val refreshToken: String,
        val twoFactorToken: String?,
        val stepUpToken: String,
        val totpSecret: String?,
        val totpRecovery: String?,
        val mailVerificationSecret: String,
        val passwordResetSecret: String,
        val sessionId: UUID
    )

    suspend fun createGroup(key: String = "test-group"): GroupDocument {
        val group = GroupDocument(key = key, translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Test")), primaryLocale = Locale.ENGLISH)
        return groupService.save(group)
    }

    suspend fun registerUser(
        email: String = "test@email.com",
        password: String = "Password#3",
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
        var stepUpToken = stepUpTokenService.create(user.id, user.sensitive.sessions.keys.first())

        if (twoFactorEnabled) {

            val twoFactorRes = webTestClient.get()
                .uri("/api/auth/2fa/totp/setup")
                .cookie(SessionTokenType.Access.cookieName, accessToken)
                .cookie(SessionTokenType.StepUp.cookieName, stepUpToken.value)
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
                .cookie(SessionTokenType.StepUp.cookieName, stepUpToken.value)
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
                .uri("/api/auth/2fa/login")
                .bodyValue(TwoFactorAuthenticationRequest(totp = gAuth.getTotpPassword(twoFactorSecret)))
                .cookie(TwoFactorTokenType.Authentication.cookieName, twoFactorToken!!)
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies

            accessToken = responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
            refreshToken = responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value

            requireNotNull(accessToken) { "No access token contained in response" }
            requireNotNull(refreshToken) { "No refresh token contained in response" }

            stepUpToken = stepUpTokenService.create(user.id, accessTokenService.extract(accessToken).sessionId)
        }

        user = userService.findById(user.id)
        
        if (roles != listOf(Role.USER)) {
            roles.forEach { role ->
                user.addRole(role)
            }
        }
        user.sensitive.groups.addAll(groups)
        user = userService.save(user)

        val mailVerificationToken = user.sensitive.security.email.verificationSecret
        val passwordResetToken = user.sensitive.security.password.resetSecret

        val sessionId = user.sensitive.sessions.keys.first()

        return TestRegisterResponse(
            user,
            email,
            password,
            accessTokenService.create(user, sessionId).value,
            refreshToken,
            twoFactorToken,
            stepUpToken.value,
            twoFactorSecret,
            twoFactorRecovery,
            mailVerificationToken,
            passwordResetToken,
            sessionId
        )
    }

    suspend fun deleteAccount(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, response.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, response.stepUpToken)
            .exchange()
            .expectStatus().isOk
    }

    suspend fun deleteAllSessions(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/api/auth/sessions")
            .cookie(SessionTokenType.Access.cookieName, response.accessToken)
            .exchange()
            .expectStatus().isOk
    }

    suspend fun EntityExchangeResult<*>.extractAccessToken(): AccessToken {
        return this.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
            ?.let { accessTokenService.extract(it) }
            ?: throw TokenException("No AccessToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractTwoFactorAuthenticationToken(): TwoFactorAuthenticationToken {
        return this.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.firstOrNull()?.value
            ?.let { twoFactorAuthenticationTokenService.extract(it) }
            ?: throw TokenException("No TwoFactorAuthenticationToken found in response")
    }
}
