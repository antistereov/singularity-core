package io.stereov.singularity.auth.guest.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.exception.model.UserAlreadyAuthenticatedException
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.guest.dto.request.ConvertToUserRequest
import io.stereov.singularity.auth.guest.dto.request.CreateGuestRequest
import io.stereov.singularity.auth.guest.dto.response.ConvertToUserResponse
import io.stereov.singularity.auth.guest.dto.response.CreateGuestResponse
import io.stereov.singularity.auth.guest.service.GuestService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.mapper.UserMapper
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
@Tag(name = "Roles")
@RequestMapping("/api/guests")
class GuestController(
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val userMapper: UserMapper,
    private val guestService: GuestService,
    private val authProperties: AuthProperties,
    private val geolocationService: GeolocationService,
    private val cookieCreator: CookieCreator,
    private val authorizationService: AuthorizationService
) {

    @PostMapping
    @Operation(
        summary = "Create Guest Account",
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
            ),
            ApiResponse(
                responseCode = "304",
                description = "User is already authenticated. Authenticated session state has not changed since last request.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun createGuestAccount(
        @RequestBody req: CreateGuestRequest,
        exchange: ServerWebExchange
    ): ResponseEntity<CreateGuestResponse> {

        if (authorizationService.isAuthenticated())
            throw UserAlreadyAuthenticatedException("Cannot create GUEST account: user is already authenticated")

        val user = guestService.createGuest(req)
        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user, sessionId, req.session, exchange)

        val res = CreateGuestResponse(
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            geolocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
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
            ),
            ApiResponse(
                responseCode = "304",
                description = "Account is already regular [`USER`](https://singularity.stereov.io/docs/guides/auth/roles#users). Authenticated session state has not changed since last request.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "`email` or `password` are invalid.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "AccessToken is invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "The email is already in use.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun convertGuestToUser(
        @RequestBody @Valid req: ConvertToUserRequest,
        @RequestParam locale: Locale?,
        exchange: ServerWebExchange
    ): ResponseEntity<ConvertToUserResponse> {

        val user = guestService.convertToUser(req, locale)
        val sessionId = authorizationService.getSessionId()

        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user, sessionId, req.session, exchange)

        val res = ConvertToUserResponse(
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            geolocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .body(res)
    }
}