package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.EmailAlreadyVerifiedException
import io.stereov.singularity.auth.core.properties.EmailVerificationProperties
import io.stereov.singularity.auth.core.service.token.EmailVerificationTokenService
import io.stereov.singularity.auth.guest.exception.model.GuestCannotPerformThisActionException
import io.stereov.singularity.email.core.exception.model.EmailCooldownException
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class EmailVerificationService(
    private val userService: UserService,
    private val authorizationService: AuthorizationService,
    private val emailVerificationTokenService: EmailVerificationTokenService,
    private val userMapper: UserMapper,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val emailProperties: EmailProperties,
    private val emailVerificationProperties: EmailVerificationProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val appProperties: AppProperties
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

        if (user.isGuest)
            throw GuestCannotPerformThisActionException("Guests cannot verify their email since no email is specified")

        if (user.sensitive.security.email.verified)
            throw EmailAlreadyVerifiedException("Email is already verified")

        if (user.sensitive.email == null)
            throw InvalidDocumentException("No email specified")

        val savedSecret = user.sensitive.security.email.verificationSecret

        return if (verificationToken.secret == savedSecret) {
            user.sensitive.security.email.verified = true
            user.sensitive.email = verificationToken.email
            val savedUser = userService.save(user)

            userMapper.toResponse(savedUser)
        } else {
            throw AuthException("Authentication token does not match")
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

        val userId = authorizationService.getUserId()
        val cooldown = getRemainingCooldown(userId)

        return MailCooldownResponse(cooldown)
    }

    /**
     * Sends an email verification token to the user.
     *
     * This method generates a verification token and sends it to the user's email address.
     */
    suspend fun sendEmailVerificationToken(locale: Locale?) {
        logger.debug { "Sending email verification token" }

        val user = authorizationService.getUser()

        if (user.isGuest)
            throw GuestCannotPerformThisActionException("Guests cannot verify their email since no email is specified")

        if (user.sensitive.security.email.verified)
            throw EmailAlreadyVerifiedException("Email is already verified")

        return sendVerificationEmail(user, locale)
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
            .setIfAbsent(key, "1", Duration.ofSeconds(emailProperties.sendCooldown))
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
        return "${emailVerificationProperties.uri}?token=$token"
    }

    /**
     * Sends a verification email to the user.
     *
     * @param user The user to send the verification email to.
     */
    suspend fun sendVerificationEmail(user: UserDocument, locale: Locale?, newEmail: String? = null) {
        logger.debug { "Sending verification email to ${newEmail ?: user.sensitive.email}" }

        val userId = user.id
        val actualLocale = locale ?: appProperties.locale

        val remainingCooldown = getRemainingCooldown(userId)
        if (remainingCooldown > 0) {
            throw EmailCooldownException(remainingCooldown)
        }

        val secret = user.sensitive.security.email.verificationSecret

        if (user.isGuest) throw GuestCannotPerformThisActionException("Failed to send verification email: a guest cannot verify an email address")

        val email = newEmail ?: user.sensitive.email
            ?: throw InvalidDocumentException("No email specified in user document")
        val token = emailVerificationTokenService.create(userId, email, secret)
        val verificationUrl = generateVerificationUrl(token)

        val slug = "email_verification"
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"), EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "verification_url" to verificationUrl
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
        startCooldown(userId)
    }

}
