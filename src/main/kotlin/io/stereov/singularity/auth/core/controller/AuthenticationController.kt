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
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.content.translate.model.Language
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

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates a user, returns an access and refresh token and sets those tokens as Http-Only Cookies.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/auth/authentication#login"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful. Returns tokens and user details.",
                content = [Content(schema = Schema(implementation = LoginResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is already authenticated.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun login(
        exchange: ServerWebExchange,
        @RequestBody payload: LoginRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<LoginResponse> {
        logger.info { "Executing login" }

        val user = authenticationService.login(payload)

        if (user.twoFactorEnabled) {
            twoFactorAuthenticationService.handleTwoFactor(user, lang)
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
        val refreshToken = refreshTokenService.create(user.id, sessionId, payload.session, exchange)

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

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account and logs them in.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/auth/authentication#registering-users"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Registration successful. Returns user details and tokens.",
                content = [Content(schema = Schema(implementation = RegisterResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Email already exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is already authenticated.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun register(
        exchange: ServerWebExchange,
        @RequestBody @Valid payload: RegisterUserRequest,
        @RequestParam("send-email") sendEmail: Boolean = true,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<RegisterResponse> {
        logger.info { "Executing register" }

        val user = authenticationService.register(payload, sendEmail, lang)
        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user.id, sessionId, payload.session, exchange)

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

    @PostMapping("/logout")
    @Operation(
        summary = "Log out a user",
        description = "Invalidates the current session's access and refresh tokens.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/auth/authentication#logout"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Logout successful.",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            )
        ]
    )
    suspend fun logout(): ResponseEntity<SuccessResponse> {
        logger.info { "Executing logout" }

        val clearAccessToken = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshToken = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpToken = cookieCreator.clearCookie(SessionTokenType.StepUp)
        val clearSessionToken = cookieCreator.clearCookie(SessionTokenType.Session)

        authenticationService.logout()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessToken.toString())
            .header("Set-Cookie", clearRefreshToken.toString())
            .header("Set-Cookie", clearStepUpToken.toString())
            .header("Set-Cookie", clearSessionToken.toString())
            .body(SuccessResponse(true))
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Refresh the access token. Returns a new access and refresh token and sets those tokens as Http-Only Cookies.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/auth/authentication#refresh"),
        security = [
            SecurityRequirement(OpenApiConstants.REFRESH_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.REFRESH_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful. Returns tokens and user details.",
                content = [Content(schema = Schema(implementation = RefreshTokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun refreshToken(
        exchange: ServerWebExchange,
        @RequestBody sessionInfo: SessionInfoRequest?
    ): ResponseEntity<RefreshTokenResponse> {
        logger.debug { "Refreshing token" }

        val refreshToken = refreshTokenService.extract(exchange)
        val user = userService.findById(refreshToken.userId)

        val newAccessToken = accessTokenService.create(user, refreshToken.sessionId)
        val newRefreshToken = refreshTokenService.create(user.id, refreshToken.sessionId, sessionInfo, exchange)

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
        summary = "Request step-up",
        description = "Requests step-up authentification. This re-authentication is required by critical endpoints.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/auth/authentication#step-up"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Logout successful.",
                content = [Content(schema = Schema(implementation = StepUpResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun stepUp(
        @RequestBody req: StepUpRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<StepUpResponse> {
        logger.info { "Executing step up request" }

        val user = authenticationService.stepUp(req)

        if (user.twoFactorEnabled) {
            twoFactorAuthenticationService.handleTwoFactor(user, lang)
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
        summary = "Get authentication status",
        description = "Get detailed information about the current status authentication status of the user.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/auth/authentication#status"),
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
                content = [Content(schema = Schema(AuthenticationStatusResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "The user contained in the TwoFactorAuthenticationToken does not exist.",
                content = [Content(schema = Schema(ErrorResponse::class))]
            )
        ]
    )
    suspend fun getStatus(exchange: ServerWebExchange): ResponseEntity<AuthenticationStatusResponse> {
        val authorizedUserId = authorizationService.getCurrentUserIdOrNull()
        val currentSessionId = authorizationService.getCurrentSessionIdOrNull()
        val twoFactorToken = runCatching { twoFactorAuthenticationTokenService.extract(exchange) }.getOrNull()

        val authorized = authorizedUserId != null
        val twoFactorRequired = twoFactorToken != null

        val stepUpToken = if (authorized && currentSessionId != null) {
            runCatching { stepUpTokenService.extract(exchange, authorizedUserId, currentSessionId) }
                .getOrNull()
        } else null
        val stepUp = stepUpToken != null

        val twoFactorMethods = if (twoFactorRequired) {
            userService.findById(twoFactorToken.userId).twoFactorMethods
        } else null
        val preferredTwoFactorMethod = if (twoFactorRequired) {
            userService.findById(twoFactorToken.userId).preferredTwoFactorMethod
        } else null

        return ResponseEntity.ok(
            AuthenticationStatusResponse(
                authorized = authorized,
                stepUp = stepUp,
                twoFactorRequired = twoFactorRequired,
                preferredTwoFactorMethod = preferredTwoFactorMethod,
                twoFactorMethods = twoFactorMethods
            )
        )
    }
}
