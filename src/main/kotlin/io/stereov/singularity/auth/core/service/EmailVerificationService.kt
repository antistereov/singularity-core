package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.translate.model.TranslateKey
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.mail.core.exception.model.MailCooldownException
import io.stereov.singularity.mail.core.properties.MailProperties
import io.stereov.singularity.mail.core.service.MailService
import io.stereov.singularity.mail.core.util.MailConstants
import io.stereov.singularity.mail.template.service.TemplateService
import io.stereov.singularity.mail.template.util.TemplateBuilder
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class EmailVerificationService(
    private val userService: UserService,
    private val authorizationService: AuthorizationService,
    private val emailVerificationTokenService: EmailVerificationTokenService,
    private val userMapper: UserMapper,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val mailProperties: MailProperties,
    private val uiProperties: UiProperties,
    private val translateService: TranslateService,
    private val mailService: MailService,
    private val templateService: TemplateService
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

        val verificationToken = emailVerificationTokenService.extract(token)
        val user = userService.findByIdOrNull(verificationToken.userId)
            ?: throw AuthException("User does not exist")

        val savedSecret = user.sensitive.security.mail.verificationSecret

        return if (verificationToken.secret == savedSecret) {
            user.sensitive.security.mail.verified = true
            user.sensitive.email = verificationToken.email
            val savedUser = userService.save(user)

            userMapper.toResponse(savedUser)
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
    suspend fun getRemainingCooldown(): MailCooldownResponse {
        logger.debug { "Getting remaining email verification cooldown" }

        val userId = authorizationService.getCurrentUserId()
        val cooldown = getRemainingCooldown(userId)

        return MailCooldownResponse(cooldown)
    }

    /**
     * Sends an email verification token to the user.
     *
     * This method generates a verification token and sends it to the user's email address.
     */
    suspend fun sendEmailVerificationToken(lang: Language) {
        logger.debug { "Sending email verification token" }

        val user = authorizationService.getCurrentUser()
        return sendVerificationEmail(user, lang)
    }

    /**
     * Gets the remaining cooldown time for email verification.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another verification email.
     *
     * @param userId The ID of the user to check the cooldown for.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingCooldown(userId: ObjectId): Long {
        logger.debug { "Getting remaining cooldown for email verification" }

        val key = "email-verification-cooldown:$userId"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    /**
     * Starts the cooldown period for email verification.
     *
     * This method sets a key in Redis to indicate that the cooldown period has started.
     *
     * @param userId The ID of the user to start the cooldown for.
     *
     * @return True if the cooldown was successfully started, false if it was already in progress.
     */
    private suspend fun startCooldown(userId: ObjectId): Boolean {
        logger.debug { "Starting cooldown for email verification" }

        val key = "email-verification-cooldown:$userId"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(mailProperties.verificationSendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }

    /**
     * Generates a verification URL for the user.
     *
     * @param token The token to include in the verification URL.
     * @return The generated verification URL.
     */
    private fun generateVerificationUrl(token: String): String {
        return "${uiProperties.baseUrl}${uiProperties.emailVerificationPath}?token=$token"
    }

    /**
     * Sends a verification email to the user.
     *
     * @param user The user to send the verification email to.
     */
    suspend fun sendVerificationEmail(user: UserDocument, lang: Language, newEmail: String? = null) {
        logger.debug { "Sending verification email to ${newEmail ?: user.sensitive.email}" }

        val userId = user.id

        val remainingCooldown = getRemainingCooldown(userId)
        if (remainingCooldown > 0) {
            throw MailCooldownException(remainingCooldown)
        }

        val secret = user.sensitive.security.mail.verificationSecret

        val email = newEmail ?: user.sensitive.email
        val token = emailVerificationTokenService.create(userId, email, secret)
        val verificationUrl = generateVerificationUrl(token)

        val slug = "email_verification"
        val templatePath = "${MailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translate(TranslateKey("$slug.subject"), MailConstants.RESOURCE_BUNDLE, lang)
        val content = TemplateBuilder.Companion.fromResource(templatePath)
            .translate(MailConstants.RESOURCE_BUNDLE, lang)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "verification_url" to verificationUrl
            )))
            .build()

        mailService.sendEmail(email, subject, content, lang)
        startCooldown(userId)
    }

}