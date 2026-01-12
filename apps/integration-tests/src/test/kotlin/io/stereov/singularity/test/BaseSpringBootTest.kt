package io.stereov.singularity.test

import com.github.michaelbull.result.getOrThrow
import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.every
import io.stereov.singularity.WebSpringBootStarterApplication
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.response.StepUpResponse
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.oauth2.properties.OAuth2Properties
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.model.*
import io.stereov.singularity.auth.token.service.*
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailProperties
import io.stereov.singularity.auth.twofactor.service.TotpService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.content.invitation.service.InvitationTokenService
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.file.core.component.DataBufferPublisher
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.download.service.DownloadService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.principal.core.dto.request.CreateGuestRequest
import io.stereov.singularity.principal.core.dto.response.CreateGuestResponse
import io.stereov.singularity.principal.core.model.Guest
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import io.stereov.singularity.principal.core.repository.UserRepository
import io.stereov.singularity.principal.core.service.GuestService
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.group.model.Group
import io.stereov.singularity.principal.group.model.GroupTranslation
import io.stereov.singularity.principal.group.repository.GroupRepository
import io.stereov.singularity.principal.group.service.GroupService
import io.stereov.singularity.test.config.MockConfig
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
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import tools.jackson.databind.json.JsonMapper
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
    lateinit var dataBufferPublisher: DataBufferPublisher

    @Autowired
    lateinit var principalService: PrincipalService

    @Autowired
    lateinit var guestService: GuestService

    @Autowired
    lateinit var downloadService: DownloadService

    @Autowired
    lateinit var uiProperties: UiProperties

    @Autowired
    lateinit var contentProperties: ContentProperties

    @Autowired
    lateinit var appProperties: AppProperties


    @Autowired
    lateinit var invitationTokenService: InvitationTokenService

    @Autowired
    lateinit var invitationService: InvitationService

    @Autowired
    lateinit var tagService: TagService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var fileStorage: FileStorage

    @Autowired
    lateinit var oAuth2Properties: OAuth2Properties

    @Autowired
    lateinit var sessionTokenService: SessionTokenService

    @Autowired
    lateinit var jsonMapper: JsonMapper

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
        principalService.deleteAll().getOrThrow()
        cacheService.deleteAll().getOrThrow()
        groupRepository.deleteAll()
        counter.set(0)
        tagService.deleteAll().getOrThrow()
        invitationService.deleteAll().getOrThrow()
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

    data class TestRegisterResponse<P : Principal<out Role, out SensitivePrincipalData>>(
        val info: P,
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
        private val accessTokenToken: AccessToken
    ) {

        val id: ObjectId
            get() = info.id.getOrThrow()

        val authentication: AuthenticationOutcome.Authenticated
            get() = AuthenticationOutcome.Authenticated(info.id.getOrThrow(),info.roles, info.groups, accessTokenToken)
    }

    suspend fun createGroup(key: String = "test-group"): Group {
        val group = Group(key = key, translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Test")))
        return groupService.save(group).getOrThrow()
    }

    suspend fun registerUser(
        emailSuffix: String = "test@email.com",
        password: String = "Password#3",
        totpEnabled: Boolean = false,
        email2faEnabled: Boolean = false,
        name: String = "Name",
        roles: List<Role> = listOf(Role.User.USER),
        groups: List<String> = listOf(),
    ): TestRegisterResponse<User> {
        val actualEmail = "${counter.getAndIncrement()}$emailSuffix"

        webTestClient.post()
            .uri("/api/auth/register?send-email=false")
            .bodyValue(RegisterUserRequest(email = actualEmail, password = password, name = name))
            .exchange()
            .expectStatus().isOk

        var twoFactorToken: String? = null
        var twoFactorRecovery: String? = null
        var twoFactorSecret: String? = null

        var user = userService.findByEmail(actualEmail).getOrThrow()

        var sessionId = UUID.randomUUID()
        var accessToken: String? = accessTokenService.create(user, sessionId).getOrThrow().value
        val refreshTokenId = Random.generateString(20).getOrThrow()
        var refreshToken: String? = refreshTokenService.create(user.id.getOrThrow(), sessionId,refreshTokenId).getOrThrow().value
        var stepUpToken = stepUpTokenService.create(user.id.getOrThrow(), sessionId).getOrThrow()

        user.updateLastActive()
        user.addOrUpdateSession(sessionId, SessionInfo(refreshTokenId = refreshTokenId))

        userService.save(user).getOrThrow()

        if (totpEnabled) {

            val twoFactorRes = webTestClient.get()
                .uri("/api/auth/2fa/totp/setup")
                .accessTokenCookie(accessToken!!)
                .stepUpTokenCookie(stepUpToken.value)
                .exchange()
                .expectStatus().isOk
                .expectBody<TwoFactorSetupResponse>()
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

            val responseCookies = webTestClient.post()
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

            sessionId = accessTokenService.extract(accessToken).getOrThrow().requireAuthentication().getOrThrow().sessionId
            stepUpToken = stepUpTokenService.create(user.id.getOrThrow(), sessionId).getOrThrow()
        }

        if (email2faEnabled) {
            if (twoFactorEmailProperties.enableByDefault) {
                twoFactorToken = twoFactorAuthenticationTokenService.create(user.id.getOrThrow()).getOrThrow().value
            } else {

                user = userService.findById(user.id.getOrThrow()).getOrThrow()
                val code = user.sensitive.security.twoFactor.email.code

                webTestClient.post()
                    .uri("/api/auth/2fa/email/enable")
                    .accessTokenCookie(accessToken!!)
                    .stepUpTokenCookie(stepUpToken.value)
                    .bodyValue(EnableEmailTwoFactorMethodRequest(code))
                    .exchange()
                    .expectStatus().isOk

                user = userService.findById(user.id.getOrThrow()).getOrThrow()

                refreshToken = refreshTokenService.create(user.id.getOrThrow(), sessionId, refreshTokenId).getOrThrow().value
                stepUpToken = stepUpTokenService.create(user.id.getOrThrow(), sessionId).getOrThrow()

                user.updateLastActive()
                user.addOrUpdateSession(sessionId, SessionInfo(refreshTokenId = refreshTokenId))

                twoFactorToken = twoFactorAuthenticationTokenService.create(user.id.getOrThrow()).getOrThrow().value
            }
        }

        user = userService.findById(user.id.getOrThrow()).getOrThrow()
        
        if (roles.contains(Role.User.ADMIN)) {
            user.addAdminRole()
        }
        user.groups.addAll(groups)
        user = userService.save(user).getOrThrow()

        val mailVerificationToken = user.sensitive.security.email.verificationSecret
        val passwordResetToken = user.sensitive.security.password.resetSecret

        return TestRegisterResponse(
            user,
            actualEmail,
            password,
            accessTokenService.create(user, sessionId).getOrThrow().value,
            refreshToken!!,
            twoFactorToken,
            stepUpToken.value,
            twoFactorSecret,
            twoFactorRecovery,
            mailVerificationToken,
            passwordResetToken,
            sessionId,
            user.sensitive.security.twoFactor.email.code,
            accessTokenService.create(user, sessionId).getOrThrow()
        )
    }

    suspend fun createAdmin(email: String = "admin@example.com"): TestRegisterResponse<User> {
        return registerUser(emailSuffix = email, roles = listOf(Role.User.USER, Role.User.ADMIN))
    }
    
    suspend fun createGuest(): TestRegisterResponse<Guest> {
        val req = CreateGuestRequest(name = "Guest", null)

        val result = webTestClient.post()
            .uri("/api/guests")
            .bodyValue(req)
            .exchange()
            .expectBody<CreateGuestResponse>()
            .returnResult()

        val accessToken = result.extractAccessToken()
        val refreshToken = result.extractRefreshToken()

        val responseBody = result.responseBody
        requireNotNull(responseBody)

        val guest = guestService.findById(responseBody.user.id).getOrThrow()

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
            email2faCode = "123456",
            accessTokenToken = accessToken
        )
    }

    suspend fun registerOAuth2(
        emailSuffix: String = "oauth2@email.com",
        provider: String = "github",
        principalId: String = "123456"
    ): TestRegisterResponse<User> {
        val actualEmail = "${counter.getAndIncrement()}$emailSuffix"
        val user = userService.save(User.ofProvider(
            email = actualEmail,
            provider = provider,
            principalId = principalId,
            mailTwoFactorCodeExpiresIn = 10,
            name = "Name"
        )).getOrThrow()

        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId).getOrThrow()
        val refreshToken = refreshTokenService.create(user.id.getOrThrow(), sessionId, "tokenId").getOrThrow().value
        val stepUpToken = stepUpTokenService.create(user.id.getOrThrow(), sessionId).getOrThrow().value

        return TestRegisterResponse(
            info = userService.save(user.copy(sensitive = user.sensitive.copy(sessions = user.sensitive.sessions.apply { put(sessionId,
                SessionInfo()) } ))).getOrThrow(),
            accessToken = accessToken.value,
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
            email2faCode = user.sensitive.security.twoFactor.email.code,
            accessTokenToken = accessToken
        )
    }

    suspend fun deleteAccount(response: TestRegisterResponse<*>) {
        webTestClient.delete()
            .uri("/api/users/me")
            .accessTokenCookie(response.accessToken)
            .stepUpTokenCookie(response.stepUpToken)
            .exchange()
            .expectStatus().isOk
    }

    suspend fun deleteAllSessions(response: TestRegisterResponse<*>) {
        webTestClient.delete()
            .uri("/api/users/me/sessions")
            .accessTokenCookie(response.accessToken)
            .exchange()
            .expectStatus().isOk
    }

    suspend fun EntityExchangeResult<*>.extractAccessToken(): AccessToken {
        return this.responseCookies[SessionTokenType.Access.cookieName]?.firstOrNull()?.value
            ?.let { accessTokenService.extract(it).getOrThrow().requireAuthentication().getOrThrow().accessToken }
            ?: throw TokenExtractionException.Missing("No AccessToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractRefreshToken(): RefreshToken {
        return this.responseCookies[SessionTokenType.Refresh.cookieName]?.firstOrNull()?.value
            ?.let { refreshTokenService.extract(it).getOrThrow() }
            ?: throw TokenExtractionException.Missing("No RefreshToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractTwoFactorAuthenticationToken(): TwoFactorAuthenticationToken {
        return this.responseCookies[TwoFactorTokenType.Authentication.cookieName]
            ?.firstOrNull()?.value
            ?.let { twoFactorAuthenticationTokenService.extract(it).getOrThrow() }
            ?: throw TokenExtractionException.Missing("No TwoFactorAuthenticationToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractStepUpToken(userId: ObjectId, sessionId: UUID): StepUpToken {
        return this.responseCookies[SessionTokenType.StepUp.cookieName]
            ?.firstOrNull()?.value
            ?.let { stepUpTokenService.extract(it, userId, sessionId).getOrThrow() }
            ?: throw TokenExtractionException.Missing("No StepUpToken found in response")
    }

    suspend fun EntityExchangeResult<*>.extractOAuth2ProviderConnectionToken(user: Principal<*,*>): OAuth2ProviderConnectionToken {
        return this.responseCookies[OAuth2TokenType.ProviderConnection.cookieName]
            ?.firstOrNull()?.value
            ?.let { oAuth2ProviderConnectionTokenService.extract(it, user).getOrThrow() }
            ?: throw TokenExtractionException.Missing("No OAuth2ProviderConnectionToken found in response")
    }

    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.accessTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(SessionTokenType.Access.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.accessTokenCookie(token: AccessToken): WebTestClient.RequestBodySpec {
        return this.accessTokenCookie(token.value)
    }
    @Suppress("UNUSED")
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.refreshTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(SessionTokenType.Refresh.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.stepUpTokenCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(SessionTokenType.StepUp.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.stepUpTokenCookie(token: StepUpToken): WebTestClient.RequestBodySpec {
        return this.stepUpTokenCookie(token.value)
    }
    @Suppress("UNUSED")
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
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.oauth2ConnectionCookie(tokenValue: String): WebTestClient.RequestBodySpec {
        return this.cookie(OAuth2TokenType.ProviderConnection.cookieName, tokenValue) as WebTestClient.RequestBodySpec
    }
    fun <S: WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.oauth2ConnectionCookie(token: OAuth2ProviderConnectionToken): WebTestClient.RequestBodySpec {
        return this.oauth2ConnectionCookie(token.value)
    }
}
