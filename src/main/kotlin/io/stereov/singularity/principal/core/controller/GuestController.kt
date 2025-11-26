package io.stereov.singularity.principal.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.exception.AccessTokenCreationException
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.token.exception.CookieException
import io.stereov.singularity.auth.token.exception.RefreshTokenCreationException
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.RefreshTokenService
import io.stereov.singularity.database.encryption.exception.EncryptedDatabaseException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.core.dto.request.ConvertToUserRequest
import io.stereov.singularity.principal.core.dto.request.CreateGuestRequest
import io.stereov.singularity.principal.core.dto.response.ConvertToUserResponse
import io.stereov.singularity.principal.core.dto.response.CreateGuestResponse
import io.stereov.singularity.principal.core.exception.ConvertGuestToUserException
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.ConvertGuestToUserService
import io.stereov.singularity.principal.core.service.GuestService
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
@Tag(name = "Roles")
@RequestMapping("/api/guests")
class GuestController(
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val principalMapper: PrincipalMapper,
    private val guestService: GuestService,
    private val authProperties: AuthProperties,
    private val geolocationService: GeolocationService,
    private val cookieCreator: CookieCreator,
    private val authorizationService: AuthorizationService,
    private val convertGuestToUserService: ConvertGuestToUserService
) {

    @PostMapping
    @Operation(
        summary = "Create Guest Principal",
        description = """
            Create a new [`GUEST`](https://singularity.stereov.io/docs/guides/auth/roles#guests) account.
            
            You can learn more about `GUEST` accounts [here](https://singularity.stereov.io/docs/guides/auth/roles#guests).
        
            **Optional session data:**
            - The `session` object can be included in the request body.
            - Inside the `session` object, you can provide the following optional fields:
                - `browser`: The name of the browser used (e.g., "Chrome", "Firefox").
                - `os`: The operating system of the device (e.g., "Windows", "macOS", "Android").
        
            This information helps users identify and manage authorized sessions, improving overall account security.
            
            ### Tokens
            
            If successful, [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
            [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
            will automatically be set as HTTP-only cookies.
            If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
            [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
            [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)
            will be returned in the response body and can be used as 
            bearer tokens in the authorization header for upcoming requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/roles#creating-guest-accounts"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Registration successful. Returns user details and tokens if header authentication is enabled.",
            )
        ]
    )
    @ThrowsDomainError([
        AuthenticationException.AlreadyAuthenticated::class,
        EncryptedDatabaseException::class,
        AccessTokenCreationException::class,
        RefreshTokenCreationException::class,
        PrincipalMapperException::class,
        CookieException.Creation::class
    ])
    suspend fun createGuestAccount(
        @RequestBody req: CreateGuestRequest,
        exchange: ServerWebExchange
    ): ResponseEntity<CreateGuestResponse> {

        if (authorizationService.isAuthenticated())
            throw AuthenticationException.AlreadyAuthenticated("Cannot create GUEST account: user is already authenticated")

        val guest = guestService.createGuest(req)
            .getOrThrow { when (it) { is EncryptedDatabaseException -> it } }

        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(guest, sessionId)
            .getOrThrow { when (it) { is AccessTokenCreationException -> it } }

        val refreshToken = refreshTokenService.create(guest, sessionId, req.session, exchange)
            .getOrThrow { when (it) { is RefreshTokenCreationException -> it } }

        val userResponse = principalMapper.toResponse(guest, AuthenticationOutcome.None())
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        val res = CreateGuestResponse(
            userResponse,
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            geolocationService.getLocationOrNull(exchange)
        )

        val accessTokenCookie = cookieCreator.createCookie(accessToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val refreshTokenCookie = cookieCreator.createCookie(refreshToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
    }

    @PostMapping("/convert-to-user")
    @Operation(
        summary = "Convert Guest To User",
        description = """
            Converts a [`GUEST`](https://singularity.stereov.io/docs/guides/auth/roles#guests) 
            account to a regular [`USER`](https://singularity.stereov.io/docs/guides/auth/roles#users) account.
            
            You can learn more about `GUEST` accounts [here](https://singularity.stereov.io/docs/guides/auth/roles#guests).
            
            ### Requirements
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
            
            ### Locale
            
            A locale can be specified for this request. 
            This will be used for the email verification email.
            You can learn more about email verification [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              of a `GUEST` is required.
            - If successful, the tokens of the `GUEST` account become invalid.
                A new [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
                [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token) 
                will automatically be set as HTTP-only cookies.
                If [header authentication](https://singularity.stereov.io/docs/guides/auth/authentication#header-authentication) is enabled,
                [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) and 
                [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)
                will be returned in the response body and can be used as 
                bearer tokens in the authorization header for upcoming requests.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/roles#creating-guest-accounts"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Registration successful. Returns user details and tokens if header authentication is enabled.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        ConvertGuestToUserException::class,
        AccessTokenCreationException::class,
        RefreshTokenCreationException::class,
        PrincipalMapperException::class,
        CookieException.Creation::class
    ])
    suspend fun convertGuestToUser(
        @RequestBody @Valid req: ConvertToUserRequest,
        @RequestParam locale: Locale?,
        exchange: ServerWebExchange
    ): ResponseEntity<ConvertToUserResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val user = convertGuestToUserService.convertToUser(authenticationOutcome.principalId, req, locale)
            .getOrThrow { when (it) { is ConvertGuestToUserException -> it } }
        val sessionId = authenticationOutcome.sessionId

        val accessToken = accessTokenService.create(user, sessionId)
            .getOrThrow { when (it) { is AccessTokenCreationException -> it } }
        val refreshToken = refreshTokenService.create(user, sessionId, req.session, exchange)
            .getOrThrow { when (it) { is RefreshTokenCreationException -> it } }

        val userResponse = principalMapper.toResponse(user, authenticationOutcome)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        val res = ConvertToUserResponse(
            userResponse,
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            geolocationService.getLocationOrNull(exchange)
        )

        val accessTokenCookie = cookieCreator.createCookie(accessToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val refreshTokenCookie = cookieCreator.createCookie(refreshToken)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
    }
}
