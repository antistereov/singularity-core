package io.stereov.singularity.auth.core.controller

import com.github.michaelbull.result.getOrThrow
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.GenerateSessionTokenResponse
import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.mapper.SessionMapper
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.SessionService
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.token.exception.CookieException
import io.stereov.singularity.auth.token.model.OAuth2TokenType
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.token.model.TwoFactorTokenType
import io.stereov.singularity.auth.token.service.SessionTokenService
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.principal.core.exception.FindPrincipalByIdException
import io.stereov.singularity.principal.core.service.PrincipalService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users/me/sessions")
@Tag(
    name = "Sessions",
    description = "Operations related to session management."
)
class SessionController(
    private val sessionService: SessionService,
    private val cookieCreator: CookieCreator,
    private val sessionTokenService: SessionTokenService,
    private val sessionMapper: SessionMapper,
    private val authorizationService: AuthorizationService,
    private val principalService: PrincipalService,
    private val accessTokenCache: AccessTokenCache,
) {

    private val logger = KotlinLogging.logger {}

    @GetMapping
    @Operation(
        summary = "Get Active Sessions",
        description = """
            Get all active sessions of the currently authenticated user.
            
            You can learn more about sessions [here](https://singularity.stereov.io/docs/guides/auth/sessions).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/sessions#active-sessions"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The list of active sessions.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindPrincipalByIdException::class
    ])
    suspend fun getActiveSessions(): ResponseEntity<List<SessionInfoResponse>> {
        val principalId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        val sessions = sessionService.getSessions(principalId)
            .getOrThrow { when (it) { is FindPrincipalByIdException -> it } }

        return ResponseEntity.ok(sessionMapper.toSessionInfoResponse(sessions))
    }

    @PostMapping("/token")
    @Operation(
        summary = "Generate SessionToken",
        description = """
            Generate a [`SessionToken`](https://singularity.stereov.io/docs/guides/auth/tokens#session-token) for the current session, if the user is authenticated or 
            a new [`SessionToken`](https://singularity.stereov.io/docs/guides/auth/tokens#session-token) instead. 
            
            It's only purpose is to successfully register or log in a user via an OAuth2 provider.
            You can learn more about OAuth2 providers [here](https://singularity.stereov.io/docs/guides/auth/oauth2).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#1-retrieving-a-session-token"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "EmailVerificationTokenCreation generated. The token will be returned if header authentication is enabled",
            )
        ]
    )
    @ThrowsDomainError([
        TokenCreationException::class,
        CookieException.Creation::class
    ])
    suspend fun generateSessionToken(
        @RequestBody sessionInfo: SessionInfoRequest?,
        @RequestParam locale: Locale?
    ): ResponseEntity<GenerateSessionTokenResponse> {
        val token = sessionTokenService.create(sessionInfo, locale = locale)
            .getOrThrow { when (it) { is TokenCreationException -> it } }

        val cookie = cookieCreator.createCookie(token, sameSite = "Lax")
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", cookie.toString())
            .body(GenerateSessionTokenResponse(token.value))
    }

    @DeleteMapping("/{sessionId}")
    @Operation(
        summary = "Delete Session",
        description = """
            Delete the session of the current user with the given `id` 
            and invalidate all tokens related to this session.
            
            You can learn more about sessions [here](https://singularity.stereov.io/docs/guides/auth/sessions).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/sessions#invalidating-a-specific-session"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The list of active sessions.",
            )
        ]

    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindPrincipalByIdException::class,
        SaveEncryptedDocumentException::class
    ])
    suspend fun deleteSession(@PathVariable sessionId: UUID): ResponseEntity<List<SessionInfoResponse>> {
        val principalId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        val principal = principalService.findById(principalId)
            .getOrThrow { when (it) { is FindPrincipalByIdException -> it }}

        val updatedPrincipal = sessionService.deleteSession(principal, sessionId)
            .getOrThrow { when (it) { is SaveEncryptedDocumentException -> it } }

        return ResponseEntity.ok(sessionMapper.toSessionInfoResponse(updatedPrincipal.sensitive.sessions))
    }

    @DeleteMapping
    @Operation(
        summary = "Delete All Sessions",
        description = """
            Invalidates all the user's active sessions.
            This also invalidates all [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)s 
            and [`RefreshToken`](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)s.
            Therefore, logging out the user from all devices.
            
            You can learn more about sessions [here](https://singularity.stereov.io/docs/guides/auth/sessions).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/sessions#invalidating-all-session"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully deleted all sessions.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired AccessToken.",
                content = [Content(schema = Schema(ErrorResponse::class))]
            )
        ],
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindPrincipalByIdException::class,
        CookieException.Creation::class,
        SaveEncryptedDocumentException::class,
        CacheException::class,
    ])
    suspend fun deleteAllSessions(): ResponseEntity<SuccessResponse> {
        logger.debug { "Logging out user from all sessions" }

        val principalId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        val principal = principalService.findById(principalId)
            .getOrThrow { when (it) { is FindPrincipalByIdException -> it }}

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

        sessionService.deleteAllSessions(principal)
            .getOrThrow { when (it) { is SaveEncryptedDocumentException -> it } }

        accessTokenCache.invalidateAllTokens(principalId)
            .getOrThrow { when (it) { is CacheException -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessToken.toString())
            .header("Set-Cookie", clearRefreshToken.toString())
            .header("Set-Cookie", clearStepUpToken.toString())
            .header("Set-Cookie", clearSessionToken.toString())
            .header("Set-Cookie", clearTwoFactorAuthenticationToken.toString())
            .header("Set-Cookie", clearOAuth2ProviderConnectionToken.toString())
            .body(SuccessResponse(true))
    }
}
