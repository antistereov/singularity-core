package io.stereov.web.user.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.mail.MailCooldownService
import io.stereov.web.global.service.mail.MailService
import io.stereov.web.global.service.mail.MailTokenService
import io.stereov.web.user.dto.MailCooldownResponse
import io.stereov.web.user.dto.ResetPasswordRequest
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.service.UserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.*

/**
 * # Service for managing user email-related operations.
 *
 * This service provides methods to verify email addresses, send verification tokens,
 * and manage email verification cooldowns.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
@ConditionalOnProperty(prefix = "baseline.mail", name = ["enable"], havingValue = "true", matchIfMissing = false)
class UserMailService(
    private val userService: UserService,
    private val authenticationService: AuthenticationService,
    private val mailCooldownService: MailCooldownService,
    private val mailService: MailService,
    private val mailTokenService: MailTokenService,
    private val hashService: HashService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Verifies the email address of the user.
     *
     * This method checks the verification token and updates the user's email verification status.
     *
     * @param token The verification token sent to the user's email.
     *
     * @return The updated user information.
     */
    suspend fun verifyEmail(token: String): UserDto {
        logger.debug { "Verifying email" }

        val verificationToken = mailTokenService.validateAndExtractVerificationToken(token)
        val user = userService.findByEmail(verificationToken.email)

        return if (user.security.mail.verificationSecret == verificationToken.secret) {
            user.security.mail.verified = true
            userService.save(user).toDto()
        } else {
            user.toDto()
        }
    }

    /**
     * Gets the remaining cooldown time for email verification.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another verification email.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingVerificationCooldown(): MailCooldownResponse {
        logger.debug { "Getting remaining email verification cooldown" }

        val userId = authenticationService.getCurrentUserId()
        val cooldown = mailCooldownService.getRemainingVerificationCooldown(userId)

        return MailCooldownResponse(cooldown)
    }

    /**
     * Sends an email verification token to the user.
     *
     * This method generates a verification token and sends it to the user's email address.
     */
    suspend fun sendEmailVerificationToken() {
        logger.debug { "Sending email verification token" }

        val user = authenticationService.getCurrentUser()
        return mailService.sendVerificationEmail(user)
    }

    /**
     * Sends a password reset email to the user.
     *
     * This method generates a password reset token and sends it to the user's email address.
     *
     * @param email The email address of the user to send the password reset email to.
     */
    suspend fun sendPasswordReset(email: String) {
        logger.debug { "Sending password reset email" }

        val user = userService.findByEmail(email)
        return mailService.sendPasswordResetEmail(user)
    }

    suspend fun resetPassword(token: String, req: ResetPasswordRequest) {
        logger.debug { "Resetting password "}

        val resetToken = mailTokenService.validateAndExtractPasswordResetToken(token)
        val user = userService.findById(resetToken.userId)

        val tokenIsValid = (user.security.mail.passwordResetSecret == resetToken.secret)

        if (!tokenIsValid) {
            throw AuthException("Password request secret does not match")
        }

        user.security.mail.passwordResetSecret = UUID.randomUUID().toString()
        user.password = hashService.hashBcrypt(req.newPassword)
        userService.save(user)
    }

    /**
     * Gets the remaining cooldown time for password reset.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another password reset email.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingPasswordResetCooldown(): MailCooldownResponse {
        logger.debug { "Getting remaining password reset cooldown" }

        val userId = authenticationService.getCurrentUserId()
        val remaining = mailCooldownService.getRemainingPasswordResetCooldown(userId)

        return MailCooldownResponse(remaining)
    }
}
