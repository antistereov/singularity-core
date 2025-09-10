package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.PasswordResetService
import io.stereov.singularity.content.translate.model.Language
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth/password")
@Tag(
    name = "Password Reset",
    description = "Operations related to password reset."
)
class PasswordResetController(
    private val passwordResetService: PasswordResetService
) {

    /**
     * Resets the password for the user using the provided token and request body.
     *
     * @param token The password reset token sent to the user's email.
     * @param req The request body containing the new password.
     *
     * @return A message indicating the success of the operation.
     */
    @PostMapping("/reset")
    suspend fun resetPassword(
        @RequestParam token: String,
        @RequestBody req: ResetPasswordRequest
    ): ResponseEntity<Map<String, String>> {
        passwordResetService.resetPassword(token, req)

        return ResponseEntity.ok()
            .body(mapOf("message" to "Successfully reset password"))
    }

    /**
     * Gets the remaining cooldown time for password reset.
     *
     * @return The remaining cooldown time in seconds.
     */
    @GetMapping("/reset/cooldown")
    suspend fun getRemainingPasswordResetCooldown(): ResponseEntity<MailCooldownResponse> {
        val remainingCooldown = passwordResetService.getRemainingCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    /**
     * Sends a password reset email to the user using the provided email address.
     *
     * @param req The email address of the user to send the password reset email to.
     *
     * @return A message indicating the success of the operation.
     */
    @PostMapping("/reset-request")
    suspend fun sendPasswordResetEmail(
        @RequestBody req: SendPasswordResetRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<Map<String, String>> {
        passwordResetService.sendPasswordReset(req, lang)

        return ResponseEntity.ok().body(
            mapOf("message" to "Successfully send password reset email")
        )
    }
}