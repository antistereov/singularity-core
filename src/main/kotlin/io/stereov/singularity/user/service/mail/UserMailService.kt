package io.stereov.singularity.user.service.mail

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.exception.AuthException
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.user.dto.UserResponse
import io.stereov.singularity.user.dto.request.ResetPasswordRequest
import io.stereov.singularity.user.dto.request.SendPasswordResetRequest
import io.stereov.singularity.user.exception.model.UserDoesNotExistException
import io.stereov.singularity.user.service.UserService
import org.springframework.stereotype.Service

/**
 * # Service for managing user email-related operations.
 *
 * This service provides methods to verify email addresses, send verification tokens,
 * and manage email verification cooldowns.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class UserMailService(
    private val userService: UserService,
    private val authenticationService: AuthenticationService,
    private val mailCooldownService: MailCooldownService,
    private val mailSender: UserMailSender,
    private val mailTokenService: MailTokenService,
    private val hashService: HashService,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Verifies the email address of the user.
     *
     * This method checks the verification token and updates the user's email verification status.
     *
     * @param token The verification token sent to the user's email.
     *
     * @return The updated user information.
     */
    suspend fun verifyEmail(token: String): UserResponse {
        logger.debug { "Verifying email" }

        val verificationToken = mailTokenService.validateAndExtractVerificationToken(token)
        val user = userService.findByIdOrNull(verificationToken.userId)
            ?: throw AuthException("User does not exist")

        val savedSecret = user.sensitive.security.mail.verificationSecret

        return if (verificationToken.secret == savedSecret) {
            user.sensitive.security.mail.verified = true
            user.sensitive.email = verificationToken.email
            userService.save(user).toResponse()
        } else {
            throw AuthException("Verification token does not match")
        }
    }

    /**
     * Gets the remaining cooldown time for email verification.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another verification email.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingVerificationCooldown(): io.stereov.singularity.user.dto.response.MailCooldownResponse {
        logger.debug { "Getting remaining email verification cooldown" }

        val userId = authenticationService.getCurrentUserId()
        val cooldown = mailCooldownService.getRemainingVerificationCooldown(userId)

        return io.stereov.singularity.user.dto.response.MailCooldownResponse(cooldown)
    }

    /**
     * Sends an email verification token to the user.
     *
     * This method generates a verification token and sends it to the user's email address.
     */
    suspend fun sendEmailVerificationToken(lang: Language) {
        logger.debug { "Sending email verification token" }

        val user = authenticationService.getCurrentUser()
        return mailSender.sendVerificationEmail(user, lang)
    }

    /**
     * Sends a password-reset email to the user.
     *
     * This method generates a password reset token and sends it to the user's email address.
     *
     * @param req The email address of the user to send the password-reset email to.
     */
    suspend fun sendPasswordReset(req: SendPasswordResetRequest, lang: Language) {
        logger.debug { "Sending password reset email" }

        try {
            val user = userService.findByEmail(req.email)
            return mailSender.sendPasswordResetEmail(user, lang)
        } catch (_: UserDoesNotExistException) {
            return
        }
    }

    suspend fun resetPassword(token: String, req: ResetPasswordRequest) {
        logger.debug { "Resetting password "}

        val resetToken = mailTokenService.validateAndExtractPasswordResetToken(token)
        val user = userService.findById(resetToken.userId)

        val savedSecret = user.sensitive.security.mail.passwordResetSecret

        val tokenIsValid = resetToken.secret == savedSecret

        if (!tokenIsValid) {
            throw AuthException("Password request secret does not match")
        }

        user.sensitive.security.mail.passwordResetSecret = Random.generateCode(20)
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
    suspend fun getRemainingPasswordResetCooldown(): io.stereov.singularity.user.dto.response.MailCooldownResponse {
        logger.debug { "Getting remaining password reset cooldown" }

        val userId = authenticationService.getCurrentUserId()
        val remaining = mailCooldownService.getRemainingPasswordResetCooldown(userId)

        return io.stereov.singularity.user.dto.response.MailCooldownResponse(remaining)
    }
}
