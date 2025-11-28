package io.stereov.singularity.auth.twofactor.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.exception.*
import io.stereov.singularity.auth.token.model.TwoFactorTokenType
import io.stereov.singularity.auth.token.service.*
import io.stereov.singularity.auth.twofactor.dto.request.TotpRecoveryRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorRecoveryResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.exception.DisableTotpException
import io.stereov.singularity.auth.twofactor.exception.GenerateTotpDetailsException
import io.stereov.singularity.auth.twofactor.exception.TotpUserRecoveryException
import io.stereov.singularity.auth.twofactor.exception.ValidateTotpSetupException
import io.stereov.singularity.auth.twofactor.service.TotpAuthenticationService
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import java.util.*

@RestController
@RequestMapping("/api/auth/2fa/totp")
@Tag(
    name = "Two-Factor Authentication"
)
class TotpAuthenticationController(
    private val totpAuthenticationService: TotpAuthenticationService,
    private val cookieCreator: CookieCreator,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val principalMapper: PrincipalMapper,
    private val authProperties: AuthProperties,
    private val authorizationService: AuthorizationService,
    private val userService: UserService,
    private val totpSetupTokenService: TotpSetupTokenService,
    private val twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService,
) {

    @GetMapping("/setup")
    @Operation(
        summary = "Get TOTP Setup Details",
        description = """
            Get a TOTP secret, recovery codes and a TOTP URL.
            This is the first step to enabling TOTP as 2FA method.
            You can learn more about this [here](https://singularity.stereov.io/docs/guides/auth/two-factor#setup).
            
            The user needs to save the recovery codes and use the URL or the secret to set up 2FA in their 2FA app.
            
            This secret will be stored inside the `token` contained in the response.
            This `token` is required to enable TOTP.
            Performing this request will not change the user's state in the database.
            Therefore, the token is the single point of truth for validation.
            Every request will generate a new TOTP secret, new recovery codes and a new TOTP URL.
            
            The setup can be completed through the endpoint 
            [`POST /api/auth/2fa/totp/setup`](https://singularity.stereov.io/docs/api/enable-totp-as-two-factor-method)
            using the `token` and a 2FA code from an authenticator app.
            
            ### Requirements
            - The user can authenticate using password. 2FA will not work with OAuth2. 
              The OAuth2 provider will validate the second factor if the user enabled it for the provider.
              
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#setup"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The TOTP secret, recovery codes, TOTP URL and setup token.",
                content = [Content(schema = Schema(implementation = TwoFactorSetupResponse::class))]
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindEncryptedDocumentByIdException::class,
        GenerateTotpDetailsException::class
    ])
    suspend fun getTotpSetupDetails(
        exchange: ServerWebExchange,
    ): ResponseEntity<TwoFactorSetupResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        authorizationService.requireStepUp(authentication, exchange)
            .getOrThrow { when (it) { is StepUpTokenExtractionException -> it } }

        val user = userService.findById(authentication.principalId)
            .getOrThrow { when (it) { is FindEncryptedDocumentByIdException -> it } }

        val res = totpAuthenticationService.getTotpDetails(user)
            .getOrThrow { when (it) { is GenerateTotpDetailsException -> it } }

        return ResponseEntity.ok().body(res)
    }

    @PostMapping("/setup")
    @Operation(
        summary = "Enable TOTP as 2FA Method",
        description = """
            Complete the TOTP setup `token` from [`GET /api/auth/2fa/setup`](https://singularity.stereov.io/docs/api/get-totp-setup-details) 
            and a TOTP code from an authenticator app.
            
            You can learn more about this [here](https://singularity.stereov.io/docs/guides/auth/two-factor#setup).
            
            A [security alert](https://singularity.stereov.io/docs/guides/auth/security-alerts#2fa-specific-alerts)
            will be sent to the user's email if this setting is enabled and
            email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            ### Requirements
            - The user can authenticate using password. 2FA will not work with OAuth2. 
              The OAuth2 provider will validate the second factor if the user enabled it for the provider.
              
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#setup"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = TwoFactorSetupResponse::class))]
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindEncryptedDocumentByIdException::class,
        TotpSetupTokenExtractionException::class,
        ValidateTotpSetupException::class,
        PrincipalMapperException::class
    ])
    suspend fun enableTotpAsTwoFactorMethod(
        @RequestBody setupRequest: TwoFactorVerifySetupRequest,
        @RequestParam locale: Locale?,
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

        val token = totpSetupTokenService.validate(setupRequest.token, authentication.principalId)
            .getOrThrow { when (it) { is TotpSetupTokenExtractionException -> it } }

        user = totpAuthenticationService.validateSetup(token, setupRequest.code, user, locale)
            .getOrThrow { when (it) { is ValidateTotpSetupException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping
    @Operation(
        summary = "Disable TOTP as 2FA Method",
        description = """
            Disable TOTP for the current user.
            
            A [security alert](https://singularity.stereov.io/docs/guides/auth/security-alerts#2fa-specific-alerts)
            will be sent to the user's email if this setting is enabled and
            email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            You can learn more about this [here](https://singularity.stereov.io/docs/guides/auth/two-factor#setup).
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#disable"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindEncryptedDocumentByIdException::class,
        DisableTotpException::class,
        PrincipalMapperException::class
    ])
    suspend fun disableTotpAsTwoFactorMethod(
        @RequestParam locale: Locale?,
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

        user = totpAuthenticationService.disable(user, locale)
            .getOrThrow { when (it) { is DisableTotpException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }

    @PostMapping("/recover")
    @Operation(
        summary = "Recover From TOTP",
        description = """
            Recover the user if they lost access to their 2FA device using a recovery `code`.
            Each `code` is only valid once.
            
            You can learn more about recovery for TOTP [here](https://singularity.stereov.io/docs/guides/auth/two-factor#recovery).
            
            ### Requirements
            - The user can authenticate using password.
            - The user enabled TOTP as 2FA method.
            
            **Optional session data:**
            - The `session` object can be included in the request body.
            - Inside the `session` object, you can provide the following optional fields:
                - `browser`: The name of the browser used (e.g., "Chrome", "Firefox").
                - `os`: The operating system of the device (e.g., "Windows", "macOS", "Android").
            
            ### Tokens
            - A valid [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token)
              is required. This token will be set automatically as HTTP-only cookie through 
              [`POST /api/auth/login`](https://singularity.stereov.io/docs/api/login) or
              [`POST /api/auth/step-up`](https://singularity.stereov.io/docs/api/step-up)
              or can be retrieved from any of those endpoints' response and set as header manually if [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) 
              is enabled.
            - If this action is successful, [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token), 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) and
              [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              will automatically be set as HTTP-only cookies.
              
              If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
              [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token), 
              [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) and
              [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              will be returned in the response body and can be used in the authorization header for upcoming requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#recovery"),
        security = [
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Recovery successful. If header authentication is enabled, " +
                        "the response will contain all tokens.",
                content = [Content(schema = Schema(implementation = TwoFactorRecoveryResponse::class))]
            ),
        ]
    )
    @ThrowsDomainError([
        TwoFactorAuthenticationTokenExtractionException::class,
        TotpUserRecoveryException::class,
        CookieException.Creation::class,
        AccessTokenCreationException::class,
        RefreshTokenCreationException::class,
        StepUpTokenCreationException::class,
        PrincipalMapperException::class,
    ])
    suspend fun recoverFromTotp(
        exchange: ServerWebExchange,
        @RequestBody req: TotpRecoveryRequest
    ): ResponseEntity<TwoFactorRecoveryResponse> {

        val token = twoFactorAuthenticationTokenService.extract(exchange)
            .getOrThrow { when (it) { is TwoFactorAuthenticationTokenExtractionException -> it } }

        val user = totpAuthenticationService.recoverUser(token, req.code)
            .getOrThrow { when (it) { is TotpUserRecoveryException -> it } }

        val sessionId = UUID.randomUUID()

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        val accessToken = accessTokenService.create(user, sessionId)
            .getOrThrow { when (it) { is AccessTokenCreationException -> it } }
        val refreshToken = refreshTokenService.create(user, sessionId,req.session, exchange)
            .getOrThrow { when (it) { is RefreshTokenCreationException -> it } }
        val stepUpToken = stepUpTokenService.createForRecovery(token.userId, sessionId, exchange)
            .getOrThrow { when (it) { is StepUpTokenCreationException -> it } }
        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it} }

        val res = TwoFactorRecoveryResponse(
            response,
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            if (authProperties.allowHeaderAuthentication) stepUpToken.value else null,
        )

        val accessTokenCookie = cookieCreator.createCookie(accessToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val refreshTokenCookie = cookieCreator.createCookie(refreshToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val stepUpTokenCookie = cookieCreator.createCookie(stepUpToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .header("Set-Cookie", stepUpTokenCookie.toString())
            .body(res)
    }
}
