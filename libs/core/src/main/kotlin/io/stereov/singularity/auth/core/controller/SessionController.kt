package io.stereov.singularity.auth.core.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.GenerateSessionTokenResponse
import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import io.stereov.singularity.auth.core.mapper.SessionMapper
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.service.SessionService
import io.stereov.singularity.auth.core.service.token.SessionTokenService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
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
@RequestMapping("/api/auth/sessions")
@Tag(
    name = "Sessions",
    description = "Operations related to session management."
)
class SessionController(
    private val sessionService: SessionService,
    private val cookieCreator: CookieCreator,
    private val sessionTokenService: SessionTokenService,
    private val sessionMapper: SessionMapper,
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
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired AccessToken.",
                content = [Content(schema = Schema(ErrorResponse::class))]
            )
        ]
    )
    suspend fun getActiveSessions(): ResponseEntity<List<SessionInfoResponse>> {
        val sessions = sessionService.getSessions()

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
                description = "Token generated. The token will be returned if header authentication is enabled",
            )
        ]
    )
    suspend fun generateSessionToken(
        @RequestBody sessionInfo: SessionInfoRequest?,
        @RequestParam locale: Locale?
    ): ResponseEntity<GenerateSessionTokenResponse> {
        val token = sessionTokenService.create(sessionInfo, locale = locale)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(token, sameSite = "Lax").toString())
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
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired AccessToken.",
                content = [Content(schema = Schema(ErrorResponse::class))]
            )
        ]

    )
    suspend fun deleteSession(@PathVariable sessionId: UUID): ResponseEntity<List<SessionInfoResponse>> {
        val updatedUser = sessionService.deleteSession(sessionId)

        return ResponseEntity.ok(sessionMapper.toSessionInfoResponse(updatedUser.sensitive.sessions))
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
    suspend fun deleteAllSessions(): ResponseEntity<SuccessResponse> {
        logger.debug { "Logging out user from all sessions" }

        val clearAccessToken = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshToken = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpToken = cookieCreator.clearCookie(SessionTokenType.StepUp)

        sessionService.deleteAllSessions()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessToken.toString())
            .header("Set-Cookie", clearRefreshToken.toString())
            .header("Set-Cookie", clearStepUpToken.toString())
            .body(SuccessResponse(true))
    }
}
