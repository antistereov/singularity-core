package io.stereov.singularity.core.user.controller

import io.stereov.singularity.core.user.dto.UserDto
import io.stereov.singularity.core.user.dto.request.ResetPasswordRequest
import io.stereov.singularity.core.user.dto.request.SendPasswordResetRequest
import io.stereov.singularity.core.user.dto.response.MailCooldownResponse
import io.stereov.singularity.core.user.service.mail.UserMailService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

/**
 * # UserMailController class.
 *
 * Controller for handling user email verification and password reset requests.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Controller
@RequestMapping("/api/user/mail")
class UserMailController(
    private val userMailService: UserMailService,
) {

    /**
     * Verifies the email address of the user using the provided token.
     *
     * @param token The verification token sent to the user's email.
     *
     * @return The updated user information.
     */
    @PostMapping("/verify")
    suspend fun verifyEmail(@RequestParam token: String): ResponseEntity<UserDto> {
        val authInfo = userMailService.verifyEmail(token)

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
        val remainingCooldown = userMailService.getRemainingVerificationCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    /**
     * Sends a verification email to the user.
     *
     * @return A message indicating the success of the operation.
     */
    @PostMapping("/verify/send")
    suspend fun sendVerificationEmail(): ResponseEntity<Map<String, String>> {

        userMailService.sendEmailVerificationToken()

        return ResponseEntity.ok().body(
            mapOf("message" to "Successfully send verification email")
        )
    }

    /**
     * Resets the password for the user using the provided token and request body.
     *
     * @param token The password reset token sent to the user's email.
     * @param req The request body containing the new password.
     *
     * @return A message indicating the success of the operation.
     */
    @PostMapping("/reset-password")
    suspend fun resetPassword(
        @RequestParam token: String,
        @RequestBody req: ResetPasswordRequest
    ): ResponseEntity<Map<String, String>> {
        userMailService.resetPassword(token, req)

        return ResponseEntity.ok()
            .body(mapOf("message" to "Successfully reset password"))
    }

    /**
     * Gets the remaining cooldown time for password reset.
     *
     * @return The remaining cooldown time in seconds.
     */
    @GetMapping("/reset-password/cooldown")
    suspend fun getRemainingPasswordResetCooldown(): ResponseEntity<MailCooldownResponse> {
        val remainingCooldown = userMailService.getRemainingPasswordResetCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    /**
     * Sends a password reset email to the user using the provided email address.
     *
     * @param email The email address of the user to send the password reset email to.
     *
     * @return A message indicating the success of the operation.
     */
    @PostMapping("/reset-password/send")
    suspend fun sendPasswordResetEmail(@RequestBody req: SendPasswordResetRequest): ResponseEntity<Map<String, String>> {
        userMailService.sendPasswordReset(req)

        return ResponseEntity.ok().body(
            mapOf("message" to "Successfully send password reset email")
        )
    }
}
