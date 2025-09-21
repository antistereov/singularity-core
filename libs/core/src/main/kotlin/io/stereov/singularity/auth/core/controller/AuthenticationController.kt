package io.stereov.singularity.auth.core.controller

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.auth.core.dto.response.*
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.oauth2.model.token.OAuth2TokenType
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
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
    private val userMapper: UserMapper,
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
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @PostMapping("/register")
    @Operation(
        summary = "Register",
        description = """
            Registers a new user account with `email`, `password`, and `name`.
        
            **Requirements:**
            - The `email` should be a valid email address (e.g., "test@example.com")
              that is not associated to an existing account.
            - The `password` must be at least 8 characters long and include at least one uppercase letter, 
              one lowercase letter, one number, and one special character (!@#$%^&*()_+={}[]|\:;'"<>,.?/).

            **Optional session data:**
            - The `session` object can be included in the request body.
            - Inside the `session` object, you can provide the following optional fields:
                - `browser`: The name of the browser used (e.g., "Chrome", "Firefox").
                - `os`: The operating system of the device (e.g., "Windows", "macOS", "Android").
        
            This information helps users identify and manage authorized sessions, improving overall account security.
            
            **Locale:**
            
            A locale can be specified for this request. 
            This will be used for the email verification email.
            You can learn more about email verification [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            
            If successful, [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
            [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
            will automatically be set as HTTP-only cookies.
            If [header authentication](https://singularity.stereov.io/docs/guides/auth/securing-endpoints#header-authentication) is enabled,
            [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
            [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)
            will be returned in the response body and can be used as 
            bearer tokens in the authorization header for upcoming requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#registering-users"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Registration successful. Returns user details and tokens if header authentication is enabled.",
            ),
            ApiResponse(
                responseCode = "304",
                description = "User is already authenticated. Authenticated session state has not changed since last request.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "`email` or `password` are invalid.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "The email is already in use.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun register(
        exchange: ServerWebExchange,
        @RequestBody @Valid payload: RegisterUserRequest,
        @RequestParam("send-email") sendEmail: Boolean = true,
        @RequestParam locale: Locale?
    ): ResponseEntity<RegisterResponse> {
        logger.info { "Executing register" }

        val user = authenticationService.register(payload, sendEmail, locale)
        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user, sessionId, payload.session, exchange)

        val res = RegisterResponse(
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            geoLocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .body(res)
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = """
            Authenticates a user with `email` and `password`.
            
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
            if [header authentication](https://singularity.stereov.io/docs/guides/auth/securing-endpoints#header-authentication) is enabled.
            
            You can complete the login through the endpoint [`POST /api/auth/2fa/login`](https://singularity.stereov.io/docs/api/complete-login).
            
            **Locale:**
            
            A locale can be specified for this request. 
            This will be used for the email 2FA code if this method is enabled for the user.
            You can learn more about 2FA through email [here](/docs/guides/auth/two-factor#email).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - If 2FA is disabled and the request is successful, 
              [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) 
              and [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
              will automatically be set as HTTP-only cookies.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/securing-endpoints#header-authentication) is enabled,
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
            ),
            ApiResponse(
                responseCode = "304",
                description = "User is already authenticated. Authenticated session state has not changed since last request.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Trying to log in user that did not set up authentication using password.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun login(
        exchange: ServerWebExchange,
        @RequestBody payload: LoginRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<LoginResponse> {
        logger.info { "Executing login" }

        val user = authenticationService.login(payload)

        if (user.twoFactorEnabled) {
            twoFactorAuthenticationService.handleTwoFactor(user, locale)
            val twoFactorAuthenticationToken = twoFactorAuthenticationTokenService.create(user.id)

            return ResponseEntity.ok()
                .header("Set-Cookie", cookieCreator.createCookie(twoFactorAuthenticationToken).toString())
                .body(
                    LoginResponse(
                        user = userMapper.toResponse(user),
                        twoFactorRequired = true,
                        allowedTwoFactorMethods = user.twoFactorMethods,
                        twoFactorAuthenticationToken = if (authProperties.allowHeaderAuthentication) twoFactorAuthenticationToken.value else null,
                        accessToken = null,
                        refreshToken = null,
                        location = geoLocationService.getLocationOrNull(exchange.request)
                    )
                )
        }

        val sessionId = UUID.randomUUID()
        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user, sessionId, payload.session, exchange)

        val res = LoginResponse(
            user = userMapper.toResponse(user),
            accessToken = if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            refreshToken = if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            twoFactorRequired = false,
            allowedTwoFactorMethods = null,
            twoFactorAuthenticationToken = null,
            location = geoLocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .body(res)
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = """
            Invalidates the current session's [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)s 
            and [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)s.
            
            **Tokens:**
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
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid AccessToken.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun logout(): ResponseEntity<SuccessResponse> {
        logger.info { "Executing logout" }

        val clearAccessToken = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshToken = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpToken = cookieCreator.clearCookie(SessionTokenType.StepUp)
        val clearSessionToken = cookieCreator.clearCookie(SessionTokenType.Session)
        val clearTwoFactorAuthenticationToken = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)
        val clearOAuth2ProviderConnectionToken = cookieCreator.clearCookie(OAuth2TokenType.ProviderConnection)

        authenticationService.logout()

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
            
            **Tokens:**
            - Requires a valid [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token).
            - If successful, [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
              will automatically be set as HTTP-only cookies.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/securing-endpoints#header-authentication) is enabled,
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
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid RefreshToken.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun refreshAccessToken(
        exchange: ServerWebExchange,
        @RequestBody(required = false) sessionInfo: SessionInfoRequest?
    ): ResponseEntity<RefreshTokenResponse> {
        logger.debug { "Refreshing token" }

        val refreshToken = refreshTokenService.extract(exchange)
        val user = userService.findByIdOrNull(refreshToken.userId)
            ?: throw InvalidTokenException("The corresponds to a non-existing user")

        val newAccessToken = accessTokenService.create(user, refreshToken.sessionId)
        val newRefreshToken = refreshTokenService.create(user, refreshToken.sessionId, sessionInfo, exchange)

        val res = RefreshTokenResponse(
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) newAccessToken.value else null,
            if (authProperties.allowHeaderAuthentication) newRefreshToken.value else null,
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(newAccessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(newRefreshToken).toString())
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
            if [header authentication](https://singularity.stereov.io/docs/guides/auth/securing-endpoints#header-authentication) is enabled.
            
            You can complete the step-up through the endpoint [`POST /api/auth/2fa/step-up`](https://singularity.stereov.io/docs/api/complete-step-up).
            
            **Request Body:**
            
            When requesting a step-up for a [`GUEST`](https://singularity.stereov.io/docs/guides/auth/roles#guests)
            there is no way to authenticate the user.
            Therefore, no request body is required in this case.
            
            If you request a step-up for a regular [`USER`](https://singularity.stereov.io/docs/guides/auth/roles#users),
            it will result in a `400 - BAD REQUEST`.
            
            **Locale:**
            
            A locale can be specified for this request. 
            This will be used for the email 2FA code if this method is enabled for the user.
            You can learn more about 2FA through email [here](/docs/guides/auth/two-factor#email).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - Requires a valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token).
            - If 2FA is disabled and the request is successful, [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              will automatically be set as HTTP-only cookie.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/securing-endpoints#header-authentication) is enabled,
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
                description = "Logout successful.",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Trying to request step-up for user that authenticated only via OAuth2 providers or " +
                        "missing request body for authenticated users.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun stepUp(
        @RequestBody(required = false) req: StepUpRequest?,
        @RequestParam locale: Locale?
    ): ResponseEntity<StepUpResponse> {
        logger.info { "Executing step up request" }

        val user = authenticationService.stepUp(req)

        if (user.twoFactorEnabled) {
            twoFactorAuthenticationService.handleTwoFactor(user, locale)
            val twoFactorToken = twoFactorAuthenticationTokenService.create(user.id)

            return ResponseEntity.ok()
                .header("Set-Cookie", cookieCreator.createCookie(twoFactorToken).toString())
                .body(
                    StepUpResponse(
                        twoFactorRequired = true,
                        allowedTwoFactorMethods = user.twoFactorMethods,
                        twoFactorAuthenticationToken = if (authProperties.allowHeaderAuthentication) twoFactorToken.value else null,
                        stepUpToken = null,
                    )
                )
        }

        val sessionId = authorizationService.getCurrentSessionId()
        val stepUpToken = stepUpTokenService.create(user.id, sessionId)

        val res = StepUpResponse(
            twoFactorRequired = false,
            allowedTwoFactorMethods = null,
            twoFactorAuthenticationToken = null,
            stepUpToken = if (authProperties.allowHeaderAuthentication) stepUpToken.value else null
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(stepUpToken).toString())
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
            ),
            ApiResponse(
                responseCode = "404",
                description = "The user contained in the TwoFactorAuthenticationToken does not exist.",
                content = [Content(schema = Schema(ErrorResponse::class))]
            )
        ]
    )
    suspend fun getAuthenticationStatus(exchange: ServerWebExchange): ResponseEntity<AuthenticationStatusResponse> {
        val authorizedUserId = authorizationService.getCurrentUserIdOrNull()
        val currentSessionId = authorizationService.getCurrentSessionIdOrNull()
        val twoFactorToken = runCatching { twoFactorAuthenticationTokenService.extract(exchange) }.getOrNull()

        val authorized = authorizedUserId != null
        val twoFactorRequired = if (authorized) false else (twoFactorToken != null)

        val stepUpToken = if (authorized && currentSessionId != null) {
            runCatching { stepUpTokenService.extract(exchange, authorizedUserId, currentSessionId) }
                .getOrNull()
        } else null
        val stepUp = stepUpToken != null

        val (twoFactorMethods, preferredTwoFactorMethod) = if (twoFactorRequired && twoFactorToken != null) {
            val user = userService.findById(twoFactorToken.userId)

            user.twoFactorMethods to user.preferredTwoFactorMethod
        } else null to null

        val emailVerified = authorizedUserId
            ?.let { userService.findByIdOrNull(it) }
            ?.sensitive?.security?.email?.verified

        return ResponseEntity.ok(
            AuthenticationStatusResponse(
                authenticated = authorized,
                stepUp = stepUp,
                emailVerified = emailVerified,
                twoFactorRequired = twoFactorRequired,
                preferredTwoFactorMethod = preferredTwoFactorMethod,
                twoFactorMethods = twoFactorMethods
            )
        )
    }
}
