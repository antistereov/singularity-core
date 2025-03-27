package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.mail.MailService
import io.stereov.web.global.service.mail.MailVerificationCooldownService
import io.stereov.web.global.service.mail.exception.MailVerificationCooldownException
import io.stereov.web.user.dto.MailVerificationCooldownResponse
import io.stereov.web.user.dto.UserDto
import org.springframework.stereotype.Service

@Service
class UserMailVerificationService(
    private val userService: UserService,
    private val jwtService: JwtService,
    private val authenticationService: AuthenticationService,
    private val mailVerificationCooldownService: MailVerificationCooldownService,
    private val mailService: MailService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun verifyEmail(token: String): UserDto {
        logger.debug { "Verifying email" }

        val verificationToken = jwtService.validateAndExtractVerificationToken(token)
        val user = userService.findByEmail(verificationToken.email)

        return if (user.security.mail.verificationUuid == verificationToken.uuid) {
            user.security.mail.verified = true
            userService.save(user).toDto()
        } else {
            user.toDto()
        }
    }

    suspend fun getRemainingEmailVerificationCooldown(): MailVerificationCooldownResponse {
        logger.debug { "Getting remaining email verification cooldown" }

        val userId = authenticationService.getCurrentUserId()
        val cooldown = mailVerificationCooldownService.getRemainingEmailVerificationCooldown(userId)

        return MailVerificationCooldownResponse(cooldown)
    }

    suspend fun resendEmailVerificationToken() {
        logger.debug { "Resending email verification token" }

        val userId = authenticationService.getCurrentUserId()
        val remainingCooldown = mailVerificationCooldownService.getRemainingEmailVerificationCooldown(userId)

        if (remainingCooldown > 0) throw MailVerificationCooldownException(
            remainingCooldown
        )

        val user = userService.findById(userId)

        return mailService.sendVerificationEmail(user)
    }

}
