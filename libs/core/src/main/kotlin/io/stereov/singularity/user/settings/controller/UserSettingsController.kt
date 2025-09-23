package io.stereov.singularity.user.settings.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.user.settings.dto.request.ChangeUserRequest
import io.stereov.singularity.user.settings.service.UserSettingsService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users/me")
@Tag(
    name = "Profile Management",
    description = "Operations related to user settings."
)
class UserSettingsController(
    private val userMapper: UserMapper,
    private val userSettingsService: UserSettingsService,
    private val cookieCreator: CookieCreator
) {

    @PutMapping("/email")
    suspend fun changeEmail(
        @RequestBody @Valid payload: ChangeEmailRequest,
        @RequestParam locale: Locale?,
    ): ResponseEntity<UserResponse> {
        val user = userSettingsService.changeEmail(payload, locale)
        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

    @PutMapping("/password")
    suspend fun changePassword(
        @RequestBody @Valid payload: ChangePasswordRequest
    ): ResponseEntity<UserResponse> {
        val user = userSettingsService.changePassword(payload)

        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

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

    @DeleteMapping
    suspend fun delete(): ResponseEntity<Map<String, String>> {
        val clearAccessTokenCookie = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshTokenCookie = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpTokenCookie = cookieCreator.clearCookie(SessionTokenType.StepUp)

        userSettingsService.deleteUser()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.value)
            .header("Set-Cookie", clearRefreshTokenCookie.value)
            .header("Set-Cookie", clearStepUpTokenCookie.value)
            .body(mapOf("message" to "success"))
    }
}
