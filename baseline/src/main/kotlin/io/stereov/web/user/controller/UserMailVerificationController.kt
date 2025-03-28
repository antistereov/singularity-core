package io.stereov.web.user.controller

import io.stereov.web.user.dto.MailVerificationCooldownResponse
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.service.UserMailVerificationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@ConditionalOnProperty(prefix = "baseline.mail", name = ["enable-verification"], havingValue = "true", matchIfMissing = false)
@RequestMapping("/user/mail")
class UserMailVerificationController(
    private val mailVerificationService: UserMailVerificationService
) {

    @GetMapping("/verify")
    suspend fun verifyEmail(@RequestParam token: String): ResponseEntity<UserDto> {
        val authInfo = mailVerificationService.verifyEmail(token)

        return ResponseEntity.ok()
            .body(authInfo)
    }

    @GetMapping("/cooldown")
    suspend fun getRemainingEmailVerificationCooldown(): ResponseEntity<MailVerificationCooldownResponse> {
        val remainingCooldown = mailVerificationService.getRemainingEmailVerificationCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    @PostMapping("/send")
    suspend fun sendVerificationEmail(): ResponseEntity<Map<String, String>> {

        mailVerificationService.sendEmailVerificationToken()

        return ResponseEntity.ok().body(
            mapOf("message" to "Successfully send verification email")
        )
    }
}
