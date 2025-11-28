package io.stereov.singularity.auth.twofactor.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.dto.response.StepUpResponse
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.exception.*
import io.stereov.singularity.auth.token.model.TwoFactorTokenType
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.RefreshTokenService
import io.stereov.singularity.auth.token.service.StepUpTokenService
import io.stereov.singularity.auth.token.service.TwoFactorAuthenticationTokenService
import io.stereov.singularity.auth.twofactor.dto.request.ChangePreferredTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.CompleteLoginRequest
import io.stereov.singularity.auth.twofactor.dto.request.CompleteStepUpRequest
import io.stereov.singularity.auth.twofactor.exception.ChangePreferredTwoFactorMethodException
import io.stereov.singularity.auth.twofactor.exception.ValidateTwoFactorException
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.exception.PrincipalException
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
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
    private val principalMapper: PrincipalMapper,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val cookieCreator: CookieCreator,
    private val authorizationService: AuthorizationService,
    private val twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService,
    private val userService: UserService
) {

    @PostMapping("/login")
    @Operation(
        summary = "Complete Login",
        description = """
            Complete second factor for login.
            
            You can learn more about the login flow [here](https://singularity.stereov.io/docs/guides/auth/authentication#login).
            
            ### Requirements
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
            
            ### Tokens
            - A valid [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token)
              is required. This token will be set automatically as HTTP-only cookie through [`POST /api/auth/login`](https://singularity.stereov.io/docs/api/login)
              or can be retrieved from the response and set as header manually if [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) 
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
            ),
        ]
    )
    @ThrowsDomainError([
        AuthenticationException.AlreadyAuthenticated::class,
        TwoFactorAuthenticationTokenExtractionException::class,
        ValidateTwoFactorException::class,
        AccessTokenCreationException::class,
        RefreshTokenCreationException::class,
        CookieException.Creation::class,
        PrincipalMapperException::class
    ])
    suspend fun completeLogin(
        exchange: ServerWebExchange,
        @RequestBody req: CompleteLoginRequest
    ): ResponseEntity<LoginResponse> {
        if (authorizationService.isAuthenticated()) {
            throw AuthenticationException.AlreadyAuthenticated("Login not required: user is already authenticated.")
        }

        val token = twoFactorAuthenticationTokenService.extract(exchange)
            .getOrThrow { when (it) { is TwoFactorAuthenticationTokenExtractionException -> it } }

        val user = twoFactorAuthService.validateTwoFactor(token, req)
            .getOrThrow { when (it) { is ValidateTwoFactorException -> it } }

        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId)
            .getOrThrow { when (it) { is AccessTokenCreationException -> it } }
        val refreshToken = refreshTokenService.create(user, sessionId, req.session, exchange)
            .getOrThrow { when (it) { is RefreshTokenCreationException -> it } }

        val accessTokenCookie = cookieCreator.createCookie(accessToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val refreshTokenCookie = cookieCreator.createCookie(refreshToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        val res = LoginResponse(
            twoFactorRequired = false,
            user = response,
            accessToken = if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            refreshToken = if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            twoFactorMethods = null,
            twoFactorAuthenticationToken = null,
            preferredTwoFactorMethod = null,
            location = geolocationService.getLocationOrNull(exchange)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
    }

    @PostMapping("/step-up")
    @Operation(
        summary = "Complete Step-Up",
        description = """
            Perform second factor for step-up.
            
            You can learn more about the step-up flow [here](https://singularity.stereov.io/docs/guides/auth/authentication#step-up).
            
            ### Requirements
            - User authenticated using their password through [`POST /api/auth/step-up`](https://singularity.stereov.io/docs/api/step-up).
            - At least one of [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) or [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp) as 2FA methods should be enabled.
            - An `email` or `totp` 2FA code is present for an enabled 2FA method.
              Check out [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) and [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp)
              to learn how to retrieve a 2FA code.
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        TwoFactorAuthenticationTokenExtractionException::class,
        ValidateTwoFactorException::class,
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        PrincipalException.InvalidDocument::class,
        StepUpTokenCreationException::class,
        CookieException.Creation::class
    ])
    suspend fun completeStepUp(
        @RequestBody req: CompleteStepUpRequest,
        exchange: ServerWebExchange
    ): ResponseEntity<StepUpResponse> {
        val token = twoFactorAuthenticationTokenService.extract(exchange)
            .getOrThrow { when (it) { is TwoFactorAuthenticationTokenExtractionException -> it } }

        val user = twoFactorAuthService.validateTwoFactor(token, req)
            .getOrThrow { when (it) { is ValidateTwoFactorException -> it } }

        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val sessionId = authentication.sessionId

        val userId = user.id
            .getOrThrow { when (it) { is PrincipalException.InvalidDocument -> it } }

        if (userId != authentication.principalId) {
            throw TwoFactorAuthenticationTokenExtractionException.Invalid(": TwoFactorAuthenticationToken does not match AccessToken")
        }

        val stepUpToken = stepUpTokenService.create(userId, sessionId)
            .getOrThrow { when (it) { is StepUpTokenCreationException -> it } }

        val res = StepUpResponse(
            stepUpToken = if (authProperties.allowHeaderAuthentication) stepUpToken.value else null,
            twoFactorRequired = false,
            twoFactorMethods = null,
            preferredTwoFactorMethod = null,
            twoFactorAuthenticationToken = null
        )

        val stepUpCookie = cookieCreator.createCookie(stepUpToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", stepUpCookie.toString())
            .body(res)
    }

    @PostMapping("/preferred-method")
    @Operation(
        summary = "Change Preferred 2FA Method",
        description = """
            Change the preferred 2FA method.
            
            You can learn more about 2FA methods [here](https://singularity.stereov.io/docs/guides/auth/two-factor).
            
            ### Requirements
            - The user can authenticate using password. 2FA will not work with OAuth2. 
              The OAuth2 provider will validate the second factor if the user enabled it for the provider.
            - At least one of [email](https://singularity.stereov.io/docs/guides/auth/two-factor#email) or [TOTP](https://singularity.stereov.io/docs/guides/auth/two-factor#totp) as 2FA methods should be enabled.
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindEncryptedDocumentByIdException::class,
        ChangePreferredTwoFactorMethodException::class,
        PrincipalMapperException::class
    ])
    suspend fun changePreferredTwoFactorMethod(
        @RequestBody req: ChangePreferredTwoFactorMethodRequest,
        exchange: ServerWebExchange,
    ): ResponseEntity<UserResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        authorizationService.requireStepUp(authentication, exchange)
            .getOrThrow { when (it) { is StepUpTokenExtractionException -> it } }

        var user = userService.findById(authentication.principalId)
            .getOrThrow { when (it) { is FindEncryptedDocumentByIdException -> it } }

        user = twoFactorAuthService.changePreferredMethod(req, user)
            .getOrThrow { when (it) { is ChangePreferredTwoFactorMethodException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it }}

        return ResponseEntity.ok(response)
    }
}
