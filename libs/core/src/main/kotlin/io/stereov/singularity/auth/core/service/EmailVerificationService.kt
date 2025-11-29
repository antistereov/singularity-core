package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.exception.SendVerificationEmailException
import io.stereov.singularity.auth.core.exception.VerifyEmailException
import io.stereov.singularity.auth.token.model.EmailVerificationToken
import io.stereov.singularity.auth.token.service.EmailVerificationTokenService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.CooldownEmailService
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.email.template.util.build
import io.stereov.singularity.email.template.util.replacePlaceholders
import io.stereov.singularity.email.template.util.translate
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Service
import java.util.*

@Service
class EmailVerificationService(
    private val userService: UserService,
    private val emailVerificationTokenService: EmailVerificationTokenService,
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
    override val slug = "email_verification"

    /**
     * Verifies a user's email using the provided token and updates the user's email or verification status as necessary.
     *
     * @param token The email verification token containing user details and a secret for validation.
     * @param locale The locale used for any locale-specific operations, such as localization of notifications, or null if not specified.
     * @return A [Result] containing the verified [User] on success or a [VerifyEmailException] on failure.
     */
    suspend fun verifyEmail(
        token: EmailVerificationToken,
        locale: Locale?
    ): Result<User, VerifyEmailException> = coroutineBinding {
        logger.debug { "Verifying email" }

        val user = userService.findById(token.userId)
            .mapError { when (it) {
                is FindEncryptedDocumentByIdException.NotFound -> VerifyEmailException.UserNotFound("User not found")
                else -> VerifyEmailException.Database("Database failure", it)
            } }
            .bind()

        val savedSecret = user.sensitive.security.email.verificationSecret

        if (token.secret != savedSecret) {
            Err(VerifyEmailException.InvalidToken("The provided token does not match the user's verification secret"))
                .bind()
        }

        val oldEmail = user.sensitive.email
        val newEmail = token.email
        val isEmailUpdate = oldEmail != newEmail

        if (isEmailUpdate) {
            user.sensitive.email = newEmail
        } else if (user.sensitive.security.email.verified) {
            Err(VerifyEmailException.AlreadyVerified("The user's email is already verified"))
                .bind()
        }

        user.sensitive.security.email.verified = true
        val savedUser = userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> VerifyEmailException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> VerifyEmailException.Database("Database failure", ex)
            } }
            .bind()

        if (isEmailUpdate && emailProperties.enable && securityAlertProperties.emailChanged) {
            securityAlertService.sendEmailChanged( oldEmail = oldEmail, newEmail = newEmail, user = savedUser, locale = locale)
                .onFailure { ex -> logger.error(ex) { "Failed to send email changed alert"} }
        }

        user
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
     * Sends a verification email to the specified user or a new email address if provided.
     *
     * @param user The user to whom the verification email should be sent.
     * @param locale The locale to use for translating email content. If null, defaults to the application's locale.
     * @param newEmail An optional new email address to send the verification email to. If null, defaults to the user's existing email.
     * @return A [Result] containing the remaining cooldown time in seconds if successful,
     * or a [SendVerificationEmailException] if an error occurs.
     */
    suspend fun sendVerificationEmail(
        user: User,
        locale: Locale?,
        newEmail: String? = null
    ): Result<Long, SendVerificationEmailException> = coroutineBinding {
        logger.debug { "Sending verification email for user ${user.id}" }

        val email = newEmail ?: user.sensitive.email
        val actualLocale = locale ?: appProperties.locale

        val cooldownActive = isCooldownActive(email)
            .mapError { ex -> SendVerificationEmailException.CooldownCache("Failed to retrieve cooldown: ${ex.message}", ex) }
            .bind()

        if (cooldownActive) {
            Err(SendVerificationEmailException.CooldownActive("Cooldown is still active for email verification"))
                .bind()
        }

        val remainingCooldown = startCooldown(email)
            .mapError { ex -> SendVerificationEmailException.CooldownCache("Failed to start cooldown: ${ex.message}", ex) }
            .bind()

        val secret = user.sensitive.security.email.verificationSecret

        if (newEmail == null && user.sensitive.security.email.verified) {
            Err(SendVerificationEmailException.AlreadyVerified("The user's email is already verified"))
                .bind()
        }

        val userId = user.id.mapError { ex -> SendVerificationEmailException.Database("Failed to retrieve user ID: ${ex.message}", ex) }
            .bind()

        val token = emailVerificationTokenService.create(userId, email, secret)
            .mapError { ex -> SendVerificationEmailException.EmailVerificationTokenCreation("Failed to create verification token: ${ex.message}", ex) }
            .bind()

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
            .mapError { ex -> SendVerificationEmailException.Template("Failed to create template for verification email: ${ex.message}", ex) }
            .bind()

        emailService.sendEmail(email, subject, content, actualLocale)
            .mapError { SendVerificationEmailException.from(it) }
            .bind()

        remainingCooldown
    }

}
