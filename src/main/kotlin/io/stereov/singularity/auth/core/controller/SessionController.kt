package io.stereov.singularity.auth.core.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import io.stereov.singularity.auth.core.model.SessionTokenType
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.core.service.SessionService
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.global.model.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth/sessions")
@Tag(
    name = "Session",
    description = "Operations related to session management"
)
class SessionController(
    private val sessionService: SessionService,
    private val cookieCreator: CookieCreator,
) {

    private val logger = KotlinLogging.logger {}

    @GetMapping
    suspend fun getSessions(): ResponseEntity<List<SessionInfoResponse>> {
        val sessions = sessionService.getSessions()

        return ResponseEntity.ok(sessions.map { it.toResponseDto() })
    }

    @DeleteMapping("/{sessionId}")
    suspend fun deleteSession(@PathVariable sessionId: String): ResponseEntity<List<SessionInfoResponse>> {
        val updatedUser = sessionService.deleteSession(sessionId)

        return ResponseEntity.ok(updatedUser.sensitive.sessions.map { it.toResponseDto()})
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
        ]
    )
    suspend fun deleteAllSessions(): ResponseEntity<SuccessResponse> {
        logger.debug { "Logging out user from all sessions" }

        val clearAccessToken = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshToken = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpToken = cookieCreator.clearCookie(TwoFactorTokenType.StepUp)

        sessionService.deleteAllSessions()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessToken.value)
            .header("Set-Cookie", clearRefreshToken.value)
            .header("Set-Cookie", clearStepUpToken.value)
            .body(SuccessResponse(true))
    }
}