package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.EmailAlreadyVerifiedException
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.token.service.EmailVerificationTokenService
import io.stereov.singularity.auth.guest.exception.model.GuestCannotPerformThisActionException
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.email.core.exception.EmailException
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.CooldownEmailService
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.email.template.util.build
import io.stereov.singularity.email.template.util.replacePlaceholders
import io.stereov.singularity.email.template.util.translate
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.PrincipalMapper
import io.stereov.singularity.user.core.model.User
import io.stereov.singularity.user.core.service.UserService
import jakarta.mail.internet.MimeMessage
import org.springframework.stereotype.Service
import java.util.*

@Service
class EmailVerificationService(
    private val userService: UserService,
    private val emailVerificationTokenService: EmailVerificationTokenService,
    private val principalMapper: PrincipalMapper,
    override val cacheService: CacheService,
    override val emailProperties: EmailProperties,
    private val uiProperties: UiProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val appProperties: AppProperties,
    private val securityAlertService: SecurityAlertService,
    private val securityAlertProperties: SecurityAlertProperties,
) : CooldownEmailService {

    override val logger = KotlinLogging.logger {}

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
            securityAlertService.sendEmailChanged( oldEmail = oldEmail, newEmail = newEmail, user = savedUser, locale = locale)
        }

        return principalMapper.toResponse(savedUser)
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
    suspend fun sendVerificationEmail(
        user: User,
        locale: Locale?,
        newEmail: String? = null
    ): Result<MimeMessage, EmailException> = coroutineBinding {
        logger.debug { "Sending verification email for user ${user.id}" }

        val email = newEmail ?: user.sensitive.email
        val actualLocale = locale ?: appProperties.locale

        isCooldownActive(email)
            .mapError { ex ->  }
            .bind()

        startCooldown(email)
            .mapError { ex -> }
            .bind()

        val secret = user.sensitive.security.email.verificationSecret

        if (newEmail == null && user.sensitive.security.email.verified)
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

        return remainingCooldown
    }

}
