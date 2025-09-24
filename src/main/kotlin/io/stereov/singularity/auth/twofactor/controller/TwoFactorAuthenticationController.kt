package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.exception.model.UserAlreadyAuthenticatedException
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.twofactor.dto.request.ChangePreferredTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.CompleteLoginRequest
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.dto.response.StepUpResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import java.util.*

@RestController
@RequestMapping("/api/auth/2fa")
@Tag(
    name = "Two-Factor Authentication",
    description = "Operations related to two-factor authentication"
)
class TwoFactorAuthenticationController(
    private val twoFactorAuthService: TwoFactorAuthenticationService,
    private val authProperties: AuthProperties,
    private val geolocationService: GeolocationService,
    private val userMapper: UserMapper,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val cookieCreator: CookieCreator,
    private val authorizationService: AuthorizationService,
) {

    @PostMapping("/login")
    @Operation(
        summary = "Complete Login",
        description = """
            Complete second factor for login.
            
            You can learn more about the login flow [here](https://singularity.stereov.io/docs/guides/auth/authentication#login).
            
            **Requirements:**
            - User authenticated using their password through [`POST /api/auth/login`](https://singularity.stereov.io/docs/api/login).
            - At least one of [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) or [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp) as 2FA methods should be enabled.
            - An `email` or `totp` 2FA code is present for an enabled 2FA method.
              Check out [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) and [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp)
              to learn how to retrieve a 2FA code.
              
            **Optional session data:**
            - The `session` object can be included in the request body.
            - Inside the `session` object, you can provide the following optional fields:
                - `browser`: The name of the browser used (e.g., "Chrome", "Firefox").
                - `os`: The operating system of the device (e.g., "Windows", "macOS", "Android").
        
            This information helps users identify and manage authorized sessions, improving overall account security.
            
            **Tokens:**
            - A valid [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token)
              is required. This token will be set automatically as HTTP-only cookie through [`POST /api/auth/login`](https://singularity.stereov.io/docs/api/login)
              or can be retrieved from the response and set as header manually if [header authentication](https://singularity.stereov.io/docs/guides/authentication#header-authentication) 
              is enabled.
            - If this action is successful, [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
              will automatically be set as HTTP-only cookies.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
              [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)
              will be returned in the response body and can be used as 
              bearer tokens in the authorization header for upcoming requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor"),
        security = [
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Information about the user and the tokens if header authentication is enabled.",
                content = [Content(schema = Schema(implementation = LoginResponse::class))]
            ),
            ApiResponse(
                responseCode = "304",
                description = "User is already authenticated.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "No 2FA code for an enabled 2FA method was provided or 2FA is disabled.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `TwoFactorAuthenticationToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun completeLogin(
        exchange: ServerWebExchange,
        @RequestBody req: CompleteLoginRequest
    ): ResponseEntity<LoginResponse> {
        if (authorizationService.isAuthenticated())
            throw UserAlreadyAuthenticatedException("Login not required: user is already authenticated.")

        val user = twoFactorAuthService.validateTwoFactor(exchange, req)
        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user, sessionId, req.session, exchange)

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)

        val res = LoginResponse(
            twoFactorRequired = false,
            user = userMapper.toResponse(user),
            accessToken = if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            refreshToken = if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            allowedTwoFactorMethods = null,
            twoFactorAuthenticationToken = null,
            preferredTwoFactorMethod = null,
            location = geolocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .body(res)
    }

    @PostMapping("/step-up")
    @Operation(
        summary = "Complete Step-Up",
        description = """
            Perform second factor for step-up.
            
            You can learn more about the step-up flow [here](https://singularity.stereov.io/docs/guides/auth/authentication#step-up).
            
            **Requirements:**
            - User authenticated using their password through [`POST /api/auth/step-up`](https://singularity.stereov.io/docs/api/step-up).
            - At least one of [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) or [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp) as 2FA methods should be enabled.
            - An `email` or `totp` 2FA code is present for an enabled 2FA method.
              Check out [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) and [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp)
              to learn how to retrieve a 2FA code.
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token)
              is required. This token should match user and session contained in the `AccessToken`.
            - If 2FA is disabled and the request is successful, [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              will automatically be set as HTTP-only cookie.
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
              the [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              will be returned in the response body and can be used to authorized critical requests.
        """,
        externalDocs = ExternalDocumentation(
            url = "https://singularity.stereov.io/docs/auth/two-factor",
        ),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The token if header authentication is enabled.",
                content = [Content(schema = Schema(implementation = StepUpResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "No 2FA code for an enabled 2FA method was provided or 2FA is disabled.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `TwoFactorAuthenticationToken` or invalid or expired 2FA code.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun completeStepUp(
        @RequestBody req: CompleteStepUpRequest,
        exchange: ServerWebExchange
    ): ResponseEntity<StepUpResponse> {
        val user = twoFactorAuthService.validateTwoFactor(exchange, req)
        val sessionId = authorizationService.getSessionId()

        if (user.id != authorizationService.getUserId())
            throw InvalidTokenException("TwoFactorAuthenticationToken does not match AccessToken")

        val stepUpToken = stepUpTokenService.create(user.id, sessionId)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(stepUpToken).toString())
            .body(StepUpResponse(if (authProperties.allowHeaderAuthentication) stepUpToken.value else null))
    }

    @PostMapping("/preferred-method")
    @Operation(
        summary = "Change Preferred 2FA Method",
        description = """
            Change the preferred 2FA method.
            
            You can learn more about 2FA methods [here](https://singularity.stereov.io/docs/guides/auth/two-factor).
            
            **Requirements:**
            - The user can authenticate using password. 2FA will not work with OAuth2. 
              The OAuth2 provider will validate the second factor if the user enabled it for the provider.
            - At least one of [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) or [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp) as 2FA methods should be enabled.
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#changing-the-preferred-method"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "This 2FA method is disabled or the user did not set up authentication via password.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` or `StepUpToken` are invalid or expired or invalid or expired 2FA code.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun changePreferredTwoFactorMethod(
        @RequestBody req: ChangePreferredTwoFactorMethodRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            userMapper.toResponse(twoFactorAuthService.updatePreferredMethod(req))
        )
    }
}
