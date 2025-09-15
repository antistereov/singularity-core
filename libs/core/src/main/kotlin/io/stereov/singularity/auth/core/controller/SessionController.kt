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
import io.stereov.singularity.global.model.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
    private val sessionMapper: SessionMapper
) {

    private val logger = KotlinLogging.logger {}

    @GetMapping
    @Operation(
        summary = "Get active sessions",
        description = "Get all active sessions of the currently authenticated user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The list of active sessions.",
                content = [Content(array = ArraySchema(schema = Schema(SessionInfoResponse::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized.",
                content = [Content(schema = Schema(ErrorResponse::class))]
            )
        ]
    )
    suspend fun getSessions(): ResponseEntity<List<SessionInfoResponse>> {
        val sessions = sessionService.getSessions()

        return ResponseEntity.ok(sessionMapper.toSessionInfoResponse(sessions))
    }

    @PostMapping("/token")
    @Operation(
        summary = "Generate SessionToken",
        description = "Generate a SessionToken for the current session, if the user is authenticated or " +
                "a new SessionToken instead. " +
                "It will be set as an HTTP-only cookie and returned in the response body if header authentication " +
                "is enabled.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Token generated. The token will be returned if header authentication is enabled",
                content = [Content(schema = Schema(implementation = GenerateSessionTokenResponse::class))]
            )
        ]
    )
    suspend fun generateSessionToken(
        @RequestBody sessionInfo: SessionInfoRequest?
    ): ResponseEntity<GenerateSessionTokenResponse> {
        val token = sessionTokenService.create(sessionInfo)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(token).toString())
            .body(GenerateSessionTokenResponse(token.value))
    }

    @DeleteMapping("/{sessionId}")
    @Operation(
        summary = "Delete a session of the current user",
        description = "Delete a session of the current user and invalidate all tokens related to this session.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The list of active sessions.",
                content = [Content(array = ArraySchema(schema = Schema(SessionInfoResponse::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized.",
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
        summary = "Log out a user from all sessions",
        description = "Invalidates all the current session's access and refresh tokens from all sessions.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Logout successful.",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
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
