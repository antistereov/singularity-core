package io.stereov.web.user.controller

import io.stereov.web.user.dto.MailVerificationCooldownResponse
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.service.UserMailVerificationService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/user/mail")
class UserMailVerificationController(
    private val mailVerificationService: UserMailVerificationService
) {

    @GetMapping("/verify-email")
    suspend fun verifyEmail(@RequestParam token: String): ResponseEntity<UserDto> {
        val authInfo = mailVerificationService.verifyEmail(token)

        return ResponseEntity.ok()
            .body(authInfo)
    }

    @GetMapping("/email-verification-cooldown")
    suspend fun getRemainingEmailVerificationCooldown(): ResponseEntity<MailVerificationCooldownResponse> {
        val remainingCooldown = mailVerificationService.getRemainingEmailVerificationCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    @PostMapping("/resend-verification-email")
    suspend fun resendVerificationEmail(): ResponseEntity<Map<String, String>> {

        mailVerificationService.resendEmailVerificationToken()

        return ResponseEntity.ok().body(
            mapOf("message" to "Successfully resend verification email")
        )
    }
}
