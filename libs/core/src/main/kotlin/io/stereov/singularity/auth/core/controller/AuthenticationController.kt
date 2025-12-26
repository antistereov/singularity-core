package io.stereov.singularity.auth.core.controller

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.LoginAlertService
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.auth.core.dto.response.AuthenticationStatusResponse
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.core.dto.response.StepUpResponse
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.exception.LoginException
import io.stereov.singularity.auth.core.exception.RegisterException
import io.stereov.singularity.auth.core.exception.StepUpException
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.exception.*
import io.stereov.singularity.auth.token.model.OAuth2TokenType
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.token.model.TwoFactorTokenType
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.RefreshTokenService
import io.stereov.singularity.auth.token.service.StepUpTokenService
import io.stereov.singularity.auth.token.service.TwoFactorAuthenticationTokenService
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.database.core.exception.DocumentException
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.global.util.getOrNull
import io.stereov.singularity.principal.core.exception.FindPrincipalByIdException
import io.stereov.singularity.principal.core.exception.FindUserByIdException
import io.stereov.singularity.principal.core.exception.PrincipalException
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import java.util.*

@RestController
@RequestMapping("/api/auth")
@Tag(
    name = "Authentication",
    description = "Operations related to user authentication."
)
class AuthenticationController(
    private val authenticationService: AuthenticationService,
    private val principalMapper: PrincipalMapper,
    private val authProperties: AuthProperties,
    private val geoLocationService: GeolocationService,
    private val twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService,
    private val cookieCreator: CookieCreator,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val userService: UserService,
    private val stepUpTokenService: StepUpTokenService,
    private val twoFactorAuthenticationService: TwoFactorAuthenticationService,
    private val authorizationService: AuthorizationService,
    private val loginAlertService: LoginAlertService,
    private val securityAlertProperties: SecurityAlertProperties,
    private val emailProperties: EmailProperties,
    private val principalService: PrincipalService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @PostMapping("/register")
    @Operation(
        summary = "Register",
        description = """
            Registers a new user account with `email`, `password`, and `name`.
            If successful, the user will receive an email with a link to verify the email address
            if email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            ### Requirements
            - The `email` should be a valid email address (e.g., "test@example.com")
              that is not associated to an existing account.
            - The `password` must be at least 8 characters long and include at least one uppercase letter, 
              one lowercase letter, one number, and one special character (!@#$%^&*()_+={}[]|\:;'"<>,.?/).

            ### Behavior for Registering Principal with Existing Email
            
            If the email is already connected to an existing account, a [warning](https://singularity.stereov.io/docs/guides/auth/security-alerts#core-identity-alerts)
            will be sent to the corresponding email address informing the user
            if email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            ### Locale
            
            A locale can be specified for this request. 
            This will be used for the email verification email.
            You can learn more about email verification [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#registering-users"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Request was successful. An email was sent to the given email address. If this email exists, a notification will be send. If the email doesn't exist, an account will be created.",
            )
        ]
    )
    @ThrowsDomainError([RegisterException::class])
    suspend fun register(
        @RequestBody @Valid payload: RegisterUserRequest,
        @RequestParam("send-email") sendEmail: Boolean = true,
        @RequestParam locale: Locale?
    ): ResponseEntity<SuccessResponse> {
        logger.info { "Executing register" }

        authenticationService.register(payload, sendEmail, locale)
            .getOrThrow { when (it) { is RegisterException -> it } }

        return ResponseEntity.ok(SuccessResponse())
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = """
            Authenticates a user with `email` and `password`.
            
            A [login alert](https://singularity.stereov.io/docs/guides/auth/security-alerts#core-identity-alerts) 
            will be sent to the user's email if this setting is enabled
            email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            If there is an account associated with the given email address but this account did not set up
            password authentication, an [Identity Provider Information](https://singularity.stereov.io/docs/guides/auth/security-alerts#no-account-information)
            email will be sent if email is enabled.
            
            **Optional session data:**
            - The `session` object can be included in the request body.
            - Inside the `session` object, you can provide the following optional fields:
                - `browser`: The name of the browser used (e.g., "Chrome", "Firefox").
                - `os`: The operating system of the device (e.g., "Windows", "macOS", "Android").
        
            This information helps users identify and manage authorized sessions, improving overall account security.
            
            **2FA:**
            
            If the user enabled 2FA, the user will not be authenticated immediately. 
            Instead, a [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token) 
            is set as HTTP-only cookie and returned in the response body
            if [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled.
            
            You can complete the login through the endpoint [`POST /api/auth/2fa/login`](https://singularity.stereov.io/docs/api/complete-login).
            
            ### Locale
            
            A locale can be specified for this request. 
            This will be used for the email 2FA code if this method is enabled for the user.
            You can learn more about 2FA through email [here](https://singularity.stereov.io/docs/guides/auth/two-factor#email).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - If 2FA is disabled and the request is successful, 
              [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) 
              and [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
              will automatically be set as HTTP-only cookies.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
              [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
              will be returned in the response body and can be used as 
              bearer tokens in the authorization header for upcoming requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#login"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful. Returns tokens and user details.",
            )
        ]
    )
    @ThrowsDomainError([
        LoginException::class,
        DocumentException.Invalid::class,
        TwoFactorAuthenticationTokenCreationException::class,
        PrincipalMapperException::class,
        CookieException.Creation::class,
        AccessTokenCreationException::class,
        RefreshTokenCreationException::class,
    ])
    suspend fun login(
        exchange: ServerWebExchange,
        @RequestBody payload: LoginRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<LoginResponse> {
        logger.info { "Executing login" }

        val user = authenticationService.login(payload, locale)
            .getOrThrow { when (it) { is LoginException -> it } }

        if (user.twoFactorEnabled) {
            twoFactorAuthenticationService.handleTwoFactor(user, locale)
                .onFailure { ex -> logger.error(ex) { "Failed to handle two factor authentication" } }

            val userId = user.id.getOrThrow { when (it) { is DocumentException.Invalid -> it }}

            val twoFactorAuthenticationToken = twoFactorAuthenticationTokenService.create(userId)
                .getOrThrow { when (it) { is TwoFactorAuthenticationTokenCreationException -> it } }

            val response = principalMapper.toResponse(user)
                .getOrThrow { when (it) { is PrincipalMapperException -> it} }

            val twoFactorAuthenticationCookie = cookieCreator.createCookie(twoFactorAuthenticationToken)
                .getOrThrow { when (it) { is CookieException.Creation -> it} }

            return ResponseEntity.ok()
                .header("Set-Cookie", twoFactorAuthenticationCookie.toString())
                .body(
                    LoginResponse(
                        user = response,
                        twoFactorRequired = true,
                        twoFactorMethods = user.twoFactorMethods,
                        twoFactorAuthenticationToken = if (authProperties.allowHeaderAuthentication) twoFactorAuthenticationToken.value else null,
                        preferredTwoFactorMethod = user.preferredTwoFactorMethod.getOrNull(),
                        accessToken = null,
                        refreshToken = null,
                        location = geoLocationService.getLocationOrNull(exchange)
                    )
                )
        }

        val sessionId = UUID.randomUUID()
        val accessToken = accessTokenService.create(user, sessionId)
            .getOrThrow { when (it) { is AccessTokenCreationException -> it} }
        val refreshToken = refreshTokenService.create(user, sessionId, payload.session, exchange)
            .getOrThrow { when (it) { is RefreshTokenCreationException -> it} }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it} }

        val res = LoginResponse(
            user = response,
            accessToken = if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            refreshToken = if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            twoFactorRequired = false,
            twoFactorMethods = null,
            twoFactorAuthenticationToken = null,
            preferredTwoFactorMethod = null,
            location = geoLocationService.getLocationOrNull(exchange)
        )

        val session = user.sensitive.sessions[sessionId]
            ?: throw PrincipalException.InvalidDocument("New session was not stored in document")

        if (securityAlertProperties.login && emailProperties.enable) {
            loginAlertService.send(user, locale, session)
                .onFailure { ex -> logger.error(ex) { "Failed to send login alert" } }
        }

        val accessTokenCookie = cookieCreator.createCookie(accessToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it} }
        val refreshTokenCookie = cookieCreator.createCookie(refreshToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it} }

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = """
            Invalidates the current session's [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)s 
            and [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)s.
            
            ### Tokens
            - To perform the logout, an [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - If successful, all cookies related to the user's session will be invalidated.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#logout"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Logout successful.",
            )
        ]
    )
    @ThrowsDomainError([
        CookieException.Creation::class,
    ])
    suspend fun logout(): ResponseEntity<SuccessResponse> {
        logger.info { "Executing logout" }

        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .get()

        val clearAccessToken = cookieCreator.clearCookie(SessionTokenType.Access)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearRefreshToken = cookieCreator.clearCookie(SessionTokenType.Refresh)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearStepUpToken = cookieCreator.clearCookie(SessionTokenType.StepUp)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearSessionToken = cookieCreator.clearCookie(SessionTokenType.Session)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearTwoFactorAuthenticationToken = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearOAuth2ProviderConnectionToken = cookieCreator.clearCookie(OAuth2TokenType.ProviderConnection)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        authenticationOutcome?.let { authenticationService.logout(authenticationOutcome) }
            ?.get()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessToken.toString())
            .header("Set-Cookie", clearRefreshToken.toString())
            .header("Set-Cookie", clearStepUpToken.toString())
            .header("Set-Cookie", clearSessionToken.toString())
            .header("Set-Cookie", clearTwoFactorAuthenticationToken.toString())
            .header("Set-Cookie", clearOAuth2ProviderConnectionToken.toString())
            .body(SuccessResponse(true))
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh Access Token",
        description = """
            Request a new [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token).
            
            ### Tokens
            - Requires a valid [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token).
            - If successful, [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
              will automatically be set as HTTP-only cookies.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
              [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)
              will be returned in the response body and can be used as 
              bearer tokens in the authorization header for upcoming requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#refresh"),
        security = [
            SecurityRequirement(OpenApiConstants.REFRESH_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.REFRESH_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful. Returns tokens and user details.",
            )
        ]
    )
    @ThrowsDomainError([
        RefreshTokenExtractionException::class,
        FindUserByIdException::class,
        AccessTokenCreationException::class,
        RefreshTokenCreationException::class,
        PrincipalMapperException::class,
        CookieException.Creation::class
    ])
    suspend fun refreshAccessToken(
        exchange: ServerWebExchange,
        @RequestBody(required = false) sessionInfo: SessionInfoRequest?
    ): ResponseEntity<RefreshTokenResponse> {
        logger.debug { "Refreshing token" }

        val refreshToken = refreshTokenService.extract(exchange)
            .getOrThrow { when (it) { is RefreshTokenExtractionException -> it } }

        val user = userService.findById(refreshToken.userId)
            .getOrThrow { FindUserByIdException.from(it) }

        val newAccessToken = accessTokenService.create(user, refreshToken.sessionId)
            .getOrThrow { when (it) { is AccessTokenCreationException -> it } }
        val newRefreshToken = refreshTokenService.create(user, refreshToken.sessionId, sessionInfo, exchange)
            .getOrThrow { when (it) { is RefreshTokenCreationException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        val res = RefreshTokenResponse(
            response,
            if (authProperties.allowHeaderAuthentication) newAccessToken.value else null,
            if (authProperties.allowHeaderAuthentication) newRefreshToken.value else null,
        )

        val accessTokenCookie = cookieCreator.createCookie(newAccessToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val refreshTokenCookie = cookieCreator.createCookie(newRefreshToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
    }

    @PostMapping("/step-up")
    @Operation(
        summary = "Step-Up",
        description = """
            Requests [step-up authentification](https://singularity.stereov.io/docs/guides/auth/authentication#step-up). 
            This re-authentication is required by critical endpoints.
            
            **2FA:**
            
            If the user enabled 2FA, the step-up will not be granted immediately. 
            Instead, a [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token) 
            is set as HTTP-only cookie and returned in the response body
            if [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled.
            
            You can complete the step-up through the endpoint [`POST /api/auth/2fa/step-up`](https://singularity.stereov.io/docs/api/complete-step-up).
            
            **Request Body:**
            
            When requesting a step-up for a [`GUEST`](https://singularity.stereov.io/docs/guides/auth/roles#guests)
            there is no way to authenticate the user.
            Therefore, no request body is required in this case.
            
            If you request a step-up for a regular [`USER`](https://singularity.stereov.io/docs/guides/auth/roles#users),
            it will result in a `400 - BAD REQUEST`.
            
            ### Locale
            
            A locale can be specified for this request. 
            This will be used for the email 2FA code if this method is enabled for the user.
            You can learn more about 2FA through email [here](/docs/guides/auth/two-factor#email).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - Requires a valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token).
            - If 2FA is disabled and the request is successful, [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              will automatically be set as HTTP-only cookie.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
              the [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              will be returned in the response body and can be used to authorized critical requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#step-up"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindPrincipalByIdException::class,
        DocumentException.Invalid::class,
        TwoFactorAuthenticationTokenCreationException::class,
        StepUpException::class,
        StepUpTokenCreationException::class,
        CookieException.Creation::class,
    ])
    suspend fun stepUp(
        @RequestBody(required = false) req: StepUpRequest?,
        @RequestParam locale: Locale?
    ): ResponseEntity<StepUpResponse> {
        logger.info { "Executing step up request" }

        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it }}
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        var principal = principalService.findById(authenticationOutcome.principalId)
            .getOrThrow { when (it) { is FindPrincipalByIdException -> it } }
        val sessionId = authenticationOutcome.sessionId

        principal = authenticationService.stepUp(principal, sessionId, req)
            .getOrThrow { when (it) { is StepUpException -> it } }
        val principalId = principal.id.getOrThrow { when (it) { is DocumentException.Invalid -> it } }

        if (principal is User && principal.twoFactorEnabled) {
            twoFactorAuthenticationService.handleTwoFactor(principal, locale)
                .onFailure { ex -> logger.error(ex) { "Failed to handle two factor authentication" } }

            val twoFactorToken = twoFactorAuthenticationTokenService.create(principalId)
                .getOrThrow { when (it) { is TwoFactorAuthenticationTokenCreationException -> it } }

            val twoFactorCookie = cookieCreator.createCookie(twoFactorToken)
                .getOrThrow { when (it) { is CookieException.Creation -> it } }

            return ResponseEntity.ok()
                .header("Set-Cookie", twoFactorCookie.toString())
                .body(
                    StepUpResponse(
                        twoFactorRequired = true,
                        twoFactorMethods = principal.twoFactorMethods,
                        twoFactorAuthenticationToken = if (authProperties.allowHeaderAuthentication) twoFactorToken.value else null,
                        stepUpToken = null,
                        preferredTwoFactorMethod = principal.preferredTwoFactorMethod.getOrNull()
                    )
                )
        }

        val stepUpToken = stepUpTokenService.create(principalId, sessionId)
            .getOrThrow { when (it) { is StepUpTokenCreationException -> it } }

        val res = StepUpResponse(
            twoFactorRequired = false,
            twoFactorMethods = null,
            twoFactorAuthenticationToken = null,
            stepUpToken = if (authProperties.allowHeaderAuthentication) stepUpToken.value else null,
            preferredTwoFactorMethod = null,
        )

        val stepUpCookie = cookieCreator.createCookie(stepUpToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", stepUpCookie.toString())
            .body(res)
    }


    @GetMapping("/status")
    @Operation(
        summary = "Get Authentication Status",
        description = """
            Get detailed information about the current status authentication status of the user.
            
            This endpoint is primarily designed for cookie-based authentication.
            *Singularity* sets HTTP-only cookies by default which cannot be accessed via JavaScript.
            Therefore, you can use this endpoint to request if valid tokens are set.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#status"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The current user's authentication status.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        FindPrincipalByIdException::class,
        PrincipalException.InvalidDocument::class,
    ])
    suspend fun getAuthenticationStatus(exchange: ServerWebExchange): ResponseEntity<AuthenticationStatusResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it }}

        val authorizedUserId = when (authenticationOutcome) {
            is AuthenticationOutcome.Authenticated -> authenticationOutcome.principalId
            is AuthenticationOutcome.None -> null
        }
        val currentSessionId = when (authenticationOutcome) {
            is AuthenticationOutcome.Authenticated -> authenticationOutcome.sessionId
            is AuthenticationOutcome.None -> null
        }
        val twoFactorToken = twoFactorAuthenticationTokenService.extract(exchange)
            .getOrNull()

        val authorized = authorizedUserId != null
        val twoFactorRequired = twoFactorToken != null

        val stepUpToken = if (authorized && currentSessionId != null) {
            stepUpTokenService.extract(exchange, authorizedUserId, currentSessionId)
                .getOrNull()
        } else null
        val stepUp = stepUpToken != null

        val principal = if (authorizedUserId != null || twoFactorToken != null) {
            principalService.findById(authorizedUserId ?: twoFactorToken!!.userId)
                .getOrThrow { when (it) { is FindPrincipalByIdException -> it } }
        } else null

        val (twoFactorMethods, preferredTwoFactorMethod) = if (principal != null && principal is User) {
            if (principal.twoFactorEnabled) {
                principal.twoFactorMethods to principal.preferredTwoFactorMethod
                    .getOrThrow { PrincipalException.InvalidDocument("2FA is enabled but no preferred method is set") }
            } else {
                null to null
            }
        } else null to null

        val emailVerified = when (principal) {
            is User -> principal.sensitive.security.email.verified
            else -> null
        }

        return ResponseEntity.ok(
            AuthenticationStatusResponse(
                authenticated = authorized,
                stepUp = stepUp,
                emailVerified = emailVerified,
                twoFactorRequired = twoFactorRequired,
                preferredTwoFactorMethod = preferredTwoFactorMethod,
                twoFactorMethods = twoFactorMethods,
                sessionId = currentSessionId
            )
        )
    }
}
