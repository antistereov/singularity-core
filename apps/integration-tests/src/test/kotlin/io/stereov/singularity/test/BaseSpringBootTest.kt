package io.stereov.singularity.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.every
import io.stereov.singularity.WebSpringBootStarterApplication
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.core.model.token.*
import io.stereov.singularity.auth.core.service.token.*
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.auth.group.repository.GroupRepository
import io.stereov.singularity.auth.group.service.GroupService
import io.stereov.singularity.auth.guest.dto.request.CreateGuestRequest
import io.stereov.singularity.auth.guest.dto.response.CreateGuestResponse
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.oauth2.model.token.OAuth2ProviderConnectionToken
import io.stereov.singularity.auth.oauth2.model.token.OAuth2TokenType
import io.stereov.singularity.auth.oauth2.service.token.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.StepUpResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorAuthenticationToken
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailProperties
import io.stereov.singularity.auth.twofactor.service.TotpService
import io.stereov.singularity.auth.twofactor.service.token.TotpSetupTokenService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.test.config.MockConfig
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
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
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [WebSpringBootStarterApplication::class]
)
@Import(MockConfig::class)
class BaseSpringBootTest() {

    @Autowired
    lateinit var sessionTokenService: SessionTokenService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService

    @Autowired
    lateinit var setupTokenService: TotpSetupTokenService

    @Autowired
    lateinit var twoFactorEmailProperties: TwoFactorEmailProperties

    @Autowired
    lateinit var cacheService: CacheService

    @Autowired
    lateinit var twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService

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

    @Autowired
    lateinit var appProperties: AppProperties

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

    @Autowired
    lateinit var groupRepository: GroupRepository

    @AfterEach
    fun clearDatabase() = runBlocking {
        userService.deleteAll()
        cacheService.deleteAll()
        groupRepository.deleteAll()
        counter.set(0)
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

    private val counter = AtomicInteger(0)

    data class TestRegisterResponse(
        val info: UserDocument,
        val email: String?,
        val password: String?,
        val accessToken: String,
        val refreshToken: String,
        val twoFactorToken: String?,
        val stepUpToken: String,
        val totpSecret: String?,
        val totpRecovery: String?,
        val mailVerificationSecret: String?,
        val passwordResetSecret: String?,
        val sessionId: UUID,
        val email2faCode: String,
    )

    suspend fun createGroup(key: String = "test-group"): GroupDocument {
        val group = GroupDocument(key = key, translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Test")), primaryLocale = Locale.ENGLISH)
        return groupService.save(group)
    }

    suspend fun registerUser(
        emailSuffix: String = "test@email.com",
        password: String = "Password#3",
        totpEnabled: Boolean = false,
        email2faEnabled: Boolean = false,
        name: String = "Name",
        roles: List<Role> = listOf(Role.USER),
        groups: List<String> = listOf(),
    ): TestRegisterResponse {
        val actualEmail = "${counter.getAndIncrement()}$emailSuffix"

        var responseCookies = webTestClient.post()
            .uri("/api/auth/register?send-email=false")
            .bodyValue(RegisterUserRequest(email = actualEmail, password = password, name = name, session = null))
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

        var user = userService.findByEmail(actualEmail)
        var stepUpToken = stepUpTokenService.create(user.id, user.sensitive.sessions.keys.first())

        if (totpEnabled) {

            val twoFactorRes = webTestClient.get()
                .uri("/api/auth/2fa/totp/setup")
                .accessTokenCookie(accessToken)
                .stepUpTokenCookie(stepUpToken.value)
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
                .accessTokenCookie(accessToken)
                .stepUpTokenCookie(stepUpToken.value)
                .bodyValue(twoFactorSetupReq)
                .exchange()
                .expectStatus().isOk

            twoFactorToken = webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(LoginRequest(actualEmail, password, null))
                .exchange()
                .expectStatus().isOk
                .returnResult<Void>()
                .responseCookies[TwoFactorTokenType.Authentication.cookieName]
                ?.firstOrNull()
                ?.value

            responseCookies = webTestClient.post()
                .uri("/api/auth/2fa/login")
                .bodyValue(CompleteStepUpRequest(totp = gAuth.getTotpPassword(twoFactorSecret)))
                .twoFactorAuthenticationTokenCookie(twoFactorToken!!)
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

        if (email2faEnabled) {
            if (twoFactorEmailProperties.enableByDefault) {
                twoFactorToken = twoFactorAuthenticationTokenService.create(user.id).value
            } else {

                user = userService.findById(user.id)
                val code = user.sensitive.security.twoFactor.email.code

                webTestClient.post()
                    .uri("/api/auth/2fa/email/enable")
                    .accessTokenCookie(accessToken)
                    .stepUpTokenCookie(stepUpToken.value)
                    .bodyValue(EnableEmailTwoFactorMethodRequest(code))
                    .exchange()
                    .expectStatus().isOk

                user = userService.findById(user.id)

                twoFactorToken = twoFactorAuthenticationTokenService.create(user.id).value
            }
        }

        user = userService.findById(user.id)
        
        if (roles != listOf(Role.USER)) {
            roles.forEach { role ->
                user.addRole(role)
            }
        }
        user.groups.addAll(groups)
        user = userService.save(user)

        val mailVerificationToken = user.sensitive.security.email.verificationSecret
        val passwordResetToken = user.sensitive.security.password.resetSecret

        val sessionId = user.sensitive.sessions.keys.first()

        return TestRegisterResponse(
            user,
            actualEmail,
            password,
            accessTokenService.create(user, sessionId).value,
            refreshToken,
            twoFactorToken,
            stepUpToken.value,
            twoFactorSecret,
            twoFactorRecovery,
            mailVerificationToken,
            passwordResetToken,
            sessionId,
            user.sensitive.security.twoFactor.email.code
        )
    }

    suspend fun createAdmin(email: String = "admin@example.com"): TestRegisterResponse {
        return registerUser(emailSuffix = email, roles = listOf(Role.USER, Role.ADMIN))
    }
    
    suspend fun createGuest(): TestRegisterResponse {
        val req = CreateGuestRequest(name = "Guest", null)

        val result = webTestClient.post()
            .uri("/api/guests")
            .bodyValue(req)
            .exchange()
            .expectBody(CreateGuestResponse::class.java)
            .returnResult()

        val accessToken = result.extractAccessToken()
        val refreshToken = result.extractRefreshToken()

        val responseBody = result.responseBody
        requireNotNull(responseBody)

        val guest = userService.findById(responseBody.user.id)

        val stepUpTokenValue = webTestClient.post()
            .uri("/api/auth/step-up")
            .accessTokenCookie(accessToken.value)
            .exchange()
            .expectStatus().isOk
            .returnResult<StepUpResponse>()
            .responseCookies[SessionTokenType.StepUp.cookieName]?.firstOrNull()
            ?.value

        requireNotNull(stepUpTokenValue)

        return TestRegisterResponse(
            info = guest,
            accessToken = accessToken.value,
            refreshToken = refreshToken.value,
            stepUpToken = stepUpTokenValue,
            email = null,
            password = null,
            twoFactorToken = null,
            totpSecret = null,
            totpRecovery = null,
            mailVerificationSecret = null,
            passwordResetSecret = null,
            sessionId = guest.sensitive.sessions.keys.first(),
            email2faCode = guest.sensitive.security.twoFactor.email.code,
        )
    }

    suspend fun registerOAuth2(
        emailSuffix: String = "oauth2@email.com",
        provider: String = "github",
        principalId: String = "123456"
    ): TestRegisterResponse {
        val actualEmail = "${counter.getAndIncrement()}$emailSuffix"
        val user = userService.save(UserDocument.ofIdentityProvider(
            email = actualEmail,
            provider = provider,
            principalId = principalId,
            mailTwoFactorCodeExpiresIn = 10,
            name = "Name"
        ))

        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId).value
        val refreshToken = refreshTokenService.create(user.id, sessionId, "tokenId").value
        val stepUpToken = stepUpTokenService.create(user.id, sessionId).value

        return TestRegisterResponse(
            info = userService.save(user.copy(sensitive = user.sensitive.copy(sessions = user.sensitive.sessions.apply { put(sessionId,
                SessionInfo()) } ))),
            accessToken = accessToken,
            refreshToken = refreshToken,
            stepUpToken = stepUpToken,
            email = actualEmail,
            password = null,
            twoFactorToken = null,
            totpSecret = null,
            totpRecovery = null,
            mailVerificationSecret = null,
            passwordResetSecret = null,
            sessionId = sessionId,
            email2faCode = user.sensitive.security.twoFactor.email.code
        )
    }

    suspend fun deleteAccount(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/api/users/me")
            .accessTokenCookie(response.accessToken)
            .stepUpTokenCookie(response.stepUpToken)
            .exchange()
            .expectStatus().isOk
    }

    suspend fun deleteAllSessions(response: TestRegisterResponse) {
        webTestClient.delete()
            .uri("/api/auth/sessions")
            .accessTokenCookie(response.accessToken)
            .exchange()
            .expectStatus().isOk
    }

    suspend fun EntityExchangeResult<*>.extractAccessToken(): AccessToken {
        return this.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
            ?.let { accessTokenService.extract(it) }
            ?: throw TokenException("No AccessToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractRefreshToken(): RefreshToken {
        return this.responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value
            ?.let { refreshTokenService.extract(it) }
            ?: throw TokenException("No RefreshToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractTwoFactorAuthenticationToken(): TwoFactorAuthenticationToken {
        return this.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.firstOrNull()?.value
            ?.let { twoFactorAuthenticationTokenService.extract(it) }
            ?: throw TokenException("No TwoFactorAuthenticationToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractStepUpToken(userId: ObjectId, sessionId: UUID): StepUpToken {
        return this.responseCookies[SessionTokenType.StepUp.cookieName]
            ?.firstOrNull()?.value
            ?.let { stepUpTokenService.extract(it, userId, sessionId) }
            ?: throw TokenException("No StepUpToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractOAuth2ProviderConnectionToken(user: UserDocument): OAuth2ProviderConnectionToken {
        return this.responseCookies[OAuth2TokenType.ProviderConnection.cookieName]
            ?.firstOrNull()?.value
            ?.let { oAuth2ProviderConnectionTokenService.extract(it, user) }
            ?: throw TokenException("No OAuth2ProviderConnectionToken found in response")
    }

    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.accessTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(SessionTokenType.Access.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.refreshTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(SessionTokenType.Refresh.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.stepUpTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(SessionTokenType.StepUp.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.twoFactorAuthenticationTokenCookie(token: TwoFactorAuthenticationToken): WebTestClient.RequestBodySpec {
        return this.twoFactorAuthenticationTokenCookie(token.value)
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.twoFactorAuthenticationTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(TwoFactorTokenType.Authentication.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.sessionTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(SessionTokenType.Session.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.sessionTokenCookie(token: SessionToken): WebTestClient.RequestBodySpec {
        return this.sessionTokenCookie(token.value)
    }
}
