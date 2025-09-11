package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth/email")
@Tag(
    name = "Email Authentication",
    description = "Operations related to email verification."
)
class EmailVerificationController(
    private val emailVerificationService: EmailVerificationService,
) {

    /**
     * Verifies the email address of the user using the provided token.
     *
     * @param token The verification token sent to the user's email.
     *
     * @return The updated user information.
     */
    @PostMapping("/verify")
    suspend fun verifyEmail(@RequestParam token: String): ResponseEntity<UserResponse> {
        val authInfo = emailVerificationService.verifyEmail(token)

        return ResponseEntity.ok()
            .body(authInfo)
    }

    /**
     * Gets the remaining cooldown time for email verification.
     *
     * @return The remaining cooldown time in seconds.
     */
    @GetMapping("/verify/cooldown")
    suspend fun getRemainingEmailVerificationCooldown(): ResponseEntity<MailCooldownResponse> {
        val remainingCooldown = emailVerificationService.getRemainingCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    /**
     * Sends a verification email to the user.
     *
     * @return A message indicating the success of the operation.
     */
    @PostMapping("/verify/send")
    suspend fun sendVerificationEmail(
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<Map<String, String>> {

        emailVerificationService.sendEmailVerificationToken(lang)

        return ResponseEntity.ok().body(
            mapOf("message" to "Successfully send verification email")
        )
    }


}