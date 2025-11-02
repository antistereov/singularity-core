package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.KillArgs.Builder.user
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.EmailAlreadyVerifiedException
import io.stereov.singularity.auth.core.model.NoAccountInfoAction
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.core.properties.SecurityAlertProperties
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
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class EmailVerificationService(
    private val userService: UserService,
    private val emailVerificationTokenService: EmailVerificationTokenService,
    private val userMapper: UserMapper,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val emailProperties: EmailProperties,
    private val uiProperties: UiProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val appProperties: AppProperties,
    private val securityAlertService: SecurityAlertService,
    private val securityAlertProperties: SecurityAlertProperties,
    private val noAccountInfoService: NoAccountInfoService
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
    suspend fun verifyEmail(token: String, locale: Locale?): UserResponse {
        logger.debug { "Verifying email" }

        val verificationToken = emailVerificationTokenService.extract(token)
        val user = userService.findByIdOrNull(verificationToken.userId)
            ?: throw AuthException("User does not exist")

        if (user.isGuest)
            throw GuestCannotPerformThisActionException("Guests cannot verify their email since no email is specified")

        if (user.sensitive.email == null)
            throw InvalidDocumentException("No email specified")

        val savedSecret = user.sensitive.security.email.verificationSecret

        if (verificationToken.secret != savedSecret) throw AuthException("Authentication token does not match")

        val oldEmail = user.sensitive.email
        val newEmail = verificationToken.email
        val isEmailUpdate = oldEmail != newEmail

        if (isEmailUpdate) {
            user.sensitive.email = newEmail
        } else if (user.sensitive.security.email.verified) {
            throw EmailAlreadyVerifiedException("Email is already verified")
        }

        user.sensitive.security.email.verified = true
        val savedUser = userService.save(user)

        if (isEmailUpdate && emailProperties.enable && securityAlertProperties.emailChanged) {
            securityAlertService.send(savedUser, locale, SecurityAlertType.EMAIL_CHANGED, oldEmail = oldEmail, newEmail = newEmail)
        }

        return userMapper.toResponse(savedUser)
    }

suspend fun getRemainingCooldown(email: String): Long {
        logger.debug { "Getting remaining cooldown for email verification" }

        val key = "email-verification-cooldown:$email"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    suspend fun startCooldown(email: String): Boolean {
        logger.debug { "Starting cooldown for email verification" }

        val key = "email-verification-cooldown:$email"
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
    private fun generateVerificationUri(token: String): String {
        return "${uiProperties.emailVerificationUri}?token=$token"
    }

    /**
     * Sends a verification email to the user.
     *
     * @param user The user to send the verification email to.
     */
    suspend fun sendVerificationEmail(email: String, locale: Locale?, sendNoAccountInfo: Boolean = false) {
        logger.debug { "Sending verification email to $email" }

        val actualLocale = locale ?: appProperties.locale

        val remainingCooldown = getRemainingCooldown(email)
        if (remainingCooldown > 0) {
            throw EmailCooldownException(remainingCooldown)
        }

        startCooldown(email)
        val user = userService.findByEmailOrNull(email)

        if (user == null) {
            if (sendNoAccountInfo) {
                logger.debug { "User with email $email not found. Sending no account info" }
                noAccountInfoService.send(email, NoAccountInfoAction.EMAIL_VERIFICATION, locale)
            }

            return
        }

        val secret = user.sensitive.security.email.verificationSecret

        if (user.isGuest)
            throw GuestCannotPerformThisActionException("Failed to send verification email: a guest cannot verify an email address")

        if (user.sensitive.security.email.verified)
            throw EmailAlreadyVerifiedException("Email is already verified")

        val token = emailVerificationTokenService.create(user.id, email, secret)
        val verificationUrl = generateVerificationUri(token)

        val slug = "email_verification"
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"), EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "verification_uri" to verificationUrl
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
    }

}
