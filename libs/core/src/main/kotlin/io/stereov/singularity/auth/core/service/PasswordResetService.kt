package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.WrongIdentityProviderException
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.service.token.PasswordResetTokenService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.translate.model.TranslateKey
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.mail.core.exception.model.MailCooldownException
import io.stereov.singularity.mail.core.properties.MailProperties
import io.stereov.singularity.mail.core.service.MailService
import io.stereov.singularity.mail.core.util.MailConstants
import io.stereov.singularity.mail.template.service.TemplateService
import io.stereov.singularity.mail.template.util.TemplateBuilder
import io.stereov.singularity.user.core.exception.model.UserDoesNotExistException
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class PasswordResetService(
    private val userService: UserService,
    private val passwordResetTokenService: PasswordResetTokenService,
    private val hashService: HashService,
    private val authorizationService: AuthorizationService,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val mailProperties: MailProperties,
    private val uiProperties: UiProperties,
    private val translateService: TranslateService,
    private val mailService: MailService,
    private val templateService: TemplateService
) {

    private val logger = KotlinLogging.logger {}

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
            return sendPasswordResetEmail(user, lang)
        } catch (_: UserDoesNotExistException) {
            return
        }
    }

    suspend fun resetPassword(token: String, req: ResetPasswordRequest) {
        logger.debug { "Resetting password "}

        val resetToken = passwordResetTokenService.extract(token)
        val user = userService.findById(resetToken.userId)

        val savedSecret = user.sensitive.security.password.resetSecret

        val tokenIsValid = resetToken.secret == savedSecret

        if (!tokenIsValid) {
            throw AuthException("Password request secret does not match")
        }

        user.sensitive.security.password.resetSecret = Random.generateString(20)
        val passwordIdentity = user.sensitive.identities.firstOrNull { it.provider == IdentityProvider.PASSWORD }
            ?: throw WrongIdentityProviderException("No password authentication is set for user")

        passwordIdentity.password = hashService.hashBcrypt(req.newPassword)
        userService.save(user)
    }

    /**
     * Gets the remaining cooldown time for password reset.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another password reset email.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingCooldown(): MailCooldownResponse {
        logger.debug { "Getting remaining password reset cooldown" }

        val userId = authorizationService.getCurrentUserId()
        val remaining = getRemainingCooldown(userId)

        return MailCooldownResponse(remaining)
    }

    /**
     * Starts the cooldown period for password reset.
     *
     * This method sets a key in Redis to indicate that the cooldown period has started.
     *
     * @param userId The ID of the user to start the cooldown for.
     *
     * @return True if the cooldown was successfully started, false if it was already in progress.
     */
    suspend fun startCooldown(userId: ObjectId): Boolean {
        logger.debug { "Starting cooldown for password reset" }

        val key = "password-reset-cooldown:$userId"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(mailProperties.sendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }

    /**
     * Gets the remaining cooldown time for password reset.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another password reset email.
     *
     * @param userId The ID of the user to check the cooldown for.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingCooldown(userId: ObjectId): Long {
        logger.debug { "Getting remaining cooldown for password resets" }

        val key = "password-reset-cooldown:$userId"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    /**
     * Sends a password reset email to the user.
     *
     * @param user The user to send the password reset email to.
     */
    private suspend fun sendPasswordResetEmail(user: UserDocument, lang: Language) {
        logger.debug { "Sending password reset email to ${user.sensitive.email}" }

        val userId = user.id

        val remainingCooldown = getRemainingCooldown(userId)
        if (remainingCooldown > 0) {
            throw MailCooldownException(remainingCooldown)
        }

        val secret = user.sensitive.security.password.resetSecret

        val token = passwordResetTokenService.create(user.id, secret)
        val passwordResetUrl = generatePasswordResetUrl(token)

        val slug = "password_reset"
        val templatePath = "${MailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translate(TranslateKey("$slug.subject"), MailConstants.RESOURCE_BUNDLE, lang)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(MailConstants.RESOURCE_BUNDLE, lang)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "reset_url" to passwordResetUrl
            )))
            .build()

        mailService.sendEmail(user.sensitive.email, subject, content, lang)
        startCooldown(userId)
    }

    /**
     * Generates a password reset URL for the user.
     *
     * @param token The token to include in the password reset URL.
     * @return The generated password reset URL.
     */
    private fun generatePasswordResetUrl(token: String): String {
        return "${uiProperties.baseUrl}${uiProperties.passwordResetPath}?token=$token"
    }
}