package io.stereov.singularity.user.settings.controller

import io.stereov.singularity.auth.core.model.SessionTokenType
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.user.settings.dto.request.ChangeUserRequest
import io.stereov.singularity.user.settings.service.UserSettingsService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/api/users/me")
@Tag(
    name = "User Settings",
    description = "Operations related to user settings."
)
class UserSettingsController(
    private val userMapper: UserMapper,
    private val userSettingsService: UserSettingsService,
    private val cookieCreator: CookieCreator
) {

    /**
     * Verify the two-factor authentication code.
     *
     * @param payload The two-factor authentication request payload.
     * @param exchange The server web exchange.
     *
     * @return The user's information as a [UserResponse].
     */
    @PutMapping("/email")
    suspend fun changeEmail(
        @RequestBody payload: ChangeEmailRequest,
        @RequestParam lang: Language = Language.EN,
        exchange: ServerWebExchange
    ): ResponseEntity<UserResponse> {
        val user = userSettingsService.changeEmail(payload, exchange, lang)
        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

    /**
     * Change the user's password.
     *
     * @param payload The change password request payload.
     * @param exchange The server web exchange.
     *
     * @return The user's information as a [UserResponse].
     */
    @PutMapping("/password")
    suspend fun changePassword(@RequestBody payload: ChangePasswordRequest, exchange: ServerWebExchange): ResponseEntity<UserResponse> {
        val user = userSettingsService.changePassword(payload, exchange)

        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

    /**
     * Change the user's information.
     *
     * @param payload The change user request payload.
     * @return The user's information as a [UserResponse].
     */
    @PutMapping
    suspend fun changeUser(@RequestBody payload: ChangeUserRequest): ResponseEntity<UserResponse> {
        val user = userSettingsService.changeUser(payload)

        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

    @PutMapping("/avatar")
    suspend fun setAvatar(@RequestPart file: FilePart): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSettingsService.setAvatar(file)
        )
    }

    @DeleteMapping("/avatar")
    suspend fun deleteAvatar(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSettingsService.deleteAvatar()
        )
    }

    /**
     * Delete the user's account.
     *
     * @return A response indicating the success of the operation.
     */
    @DeleteMapping
    suspend fun delete(): ResponseEntity<Map<String, String>> {
        val clearAccessTokenCookie = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshTokenCookie = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpTokenCookie = cookieCreator.clearCookie(TwoFactorTokenType.StepUp)

        userSettingsService.deleteUser()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.value)
            .header("Set-Cookie", clearRefreshTokenCookie.value)
            .header("Set-Cookie", clearStepUpTokenCookie.value)
            .body(mapOf("message" to "success"))
    }
}
