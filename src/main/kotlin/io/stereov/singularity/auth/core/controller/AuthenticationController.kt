package io.stereov.singularity.auth.core.controller

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.dto.response.RefreshTokenResponse
import io.stereov.singularity.auth.core.dto.response.RegisterResponse
import io.stereov.singularity.auth.core.model.SessionTokenType
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AccessTokenService
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.core.service.RefreshTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TwoFactorLoginTokenService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
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
    private val loginTokenService: TwoFactorLoginTokenService,
    private val cookieCreator: CookieCreator,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val userService: UserService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates a user, returns an access and refresh token and sets those tokens as Http-Only Cookies.",
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful. Returns tokens and user details.",
                content = [Content(schema = Schema(implementation = LoginResponse::class))]
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
        @RequestBody payload: LoginRequest
    ): ResponseEntity<LoginResponse> {
        logger.info { "Executing login" }

        val user = authenticationService.checkCredentialsAndGetUser(payload)

        if (user.sensitive.security.twoFactor.enabled) {
            val loginToken = loginTokenService.create(user.id)

            return ResponseEntity.ok()
                .header("Set-Cookie", cookieCreator.createCookie(loginToken).toString())
                .body(LoginResponse(
                    true,
                    userMapper.toResponse(user),
                    twoFactorLoginToken = if (authProperties.allowHeaderAuthentication) loginToken.value else null
                ))
        }

        val accessToken = accessTokenService.create(user.id, payload.session.id)
        val refreshToken = refreshTokenService.create(user.id, payload.session, exchange)

        val res = LoginResponse(
            false,
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            null,
            geoLocationService.getLocationOrNull(exchange.request)
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

        val user = authenticationService.registerAndGetUser(payload, sendEmail, lang)

        val accessToken = accessTokenService.create(user.id, payload.session.id)
        val refreshToken = refreshTokenService.create(user.id, payload.session, exchange)

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
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(responseCode = "200", description = "Logout successful.", content = [Content(schema = Schema(implementation = SuccessResponse::class))])
        ]
    )
    suspend fun logout(@RequestBody sessionInfo: SessionInfoRequest): ResponseEntity<SuccessResponse> {
        logger.info { "Executing logout" }

        val clearAccessToken = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshToken = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpToken = cookieCreator.clearCookie(TwoFactorTokenType.StepUp)

        authenticationService.logout(sessionInfo.id)

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessToken.value)
            .header("Set-Cookie", clearRefreshToken.value)
            .header("Set-Cookie", clearStepUpToken.value)
            .body(SuccessResponse(true))
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Refresh the access token. Returns a new access and refresh token and sets those tokens as Http-Only Cookies.",
        security = [
            SecurityRequirement(OpenApiConstants.REFRESH_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.REFRESH_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful. Returns tokens and user details.",
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
        @RequestBody sessionInfoDto: SessionInfoRequest
    ): ResponseEntity<RefreshTokenResponse> {
        logger.debug { "Refreshing token" }

        val token = refreshTokenService.extract(exchange, sessionInfoDto.id)
        val user = userService.findById(token.userId)

        val newAccessToken = accessTokenService.create(user.id, sessionInfoDto.id)
        val newRefreshToken = refreshTokenService.create(user.id, sessionInfoDto, exchange)

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
}
