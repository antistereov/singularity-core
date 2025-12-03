package io.stereov.singularity.auth.twofactor.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.exception.*
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.cache.service.CacheService
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
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * A service responsible for handling email-based authentication operations, including
 * sending and validating two-factor authentication (2FA) codes, enabling and disabling
 * email-based 2FA, and sending authentication-related emails.
 *
 * The service integrates with user management, email delivery, and caching components
 * to provide secure and localized email-based authentication functionalities. This class
 * is designed to handle exceptions and return structured results for each operation.
 */
@Service
class EmailAuthenticationService(
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val userService: UserService,
    private val translateService: TranslateService,
    private val templateService: TemplateService,
    override val cacheService: CacheService,
    private val emailService: EmailService,
    override val emailProperties: EmailProperties,
    private val accessTokenCache: AccessTokenCache,
    private val appProperties: AppProperties,
    private val securityAlertProperties: SecurityAlertProperties,
    private val securityAlertService: SecurityAlertService
) : CooldownEmailService {

    override val logger = KotlinLogging.logger {}
    override val slug = "email_authentication"

    /**
     * Sends an authentication email with a generated verification code to the specified user.
     *
     * @param user The user to whom the authentication email will be sent.
     * @param locale Optional locale to customize the email's language and content. If null, a default locale is used.
     * @return A [Result] encapsulating either success (Unit) or a [SendEmailAuthenticationException] if an error occurred.
     */
    suspend fun sendMail(
        user: User,
        locale: Locale?
    ): Result<Long, SendEmailAuthenticationException> = coroutineBinding {
        logger.debug { "Generating new code and sending email" }

        val code = Random.generateInt().getOrThrow()
        user.sensitive.security.twoFactor.email.code = code
        user.sensitive.security.twoFactor.email.expiresAt = Instant.now().plusSeconds(twoFactorEmailCodeProperties.expiresIn)

        userService.save(user)
            .mapError { ex -> SendEmailAuthenticationException.Database("Failed to save user: ${ex.message}", ex) }
            .bind()

        sendAuthenticationEmail(user, code, locale).bind()
    }

    /**
     * Validates a two-factor authentication (2FA) code for the specified user.
     *
     * @param user The user for whom the 2FA code is being validated.
     * @param code The 2FA code to validate.
     * @return A [Result] wrapping the validated [User] on success or a [ValidateTwoFactorException] on failure.
     */
    suspend fun validateCode(
        user: User,
        code: String
    ): Result<User, ValidateTwoFactorException> = coroutineBinding {
        logger.debug { "Validating 2FA code" }

        if (!user.sensitive.security.twoFactor.email.enabled) {
            Err(ValidateTwoFactorException.TwoFactorAuthenticationDisabled("Cannot validate code: 2FA method is disabled"))
                .bind()
        }

        doValidateCode(user, code)
            .mapError { ex -> ValidateTwoFactorException.fromValidateEmailTwoFactorCode(ex) }
            .bind()

        user.sensitive.security.twoFactor.email.code = Random.generateInt().getOrThrow()
        user.sensitive.security.twoFactor.email.expiresAt = Instant.now().plusSeconds(twoFactorEmailCodeProperties.expiresIn)

        userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect ->
                    ValidateTwoFactorException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> ValidateTwoFactorException.Database("Failed to save user: ${ex.message}", ex)
            } }
            .bind()
    }

    /**
     * Sends an authentication email to the specified user with a verification code.
     * Optionally considers a locale for email content localization.
     *
     * @param user The user to whom the authentication email will be sent.
     * @param code The verification code to include in the email.
     * @param locale The locale to use for email content localization, or null to use the application default.
     * @return A [Result] type wrapping a [Unit] on success or a [SendEmailAuthenticationException] on failure.
     */
    suspend fun sendAuthenticationEmail(
        user: User,
        code: String,
        locale: Locale?
    ): Result<Long, SendEmailAuthenticationException> = coroutineBinding {
        logger.debug { "Sending verification email to ${user.sensitive.email}" }

        val actualLocale = locale ?: appProperties.locale

        if (user.sensitive.identities.password == null) {
            Err(SendEmailAuthenticationException.NoPasswordSet("Cannot send email authentication: user did not set up authentication using a password."))
                .bind()
        }

        val remainingCooldown = getRemainingCooldown(user.email)
            .mapError { ex -> SendEmailAuthenticationException.CooldownCache("Failed to get remaining cooldown: ${ex.message}", ex) }
            .bind()
        if (remainingCooldown.seconds > 0) {
            Err(SendEmailAuthenticationException.CooldownActive("Cannot send email authentication: cooldown is still active"))
                .bind()
        }

        val slug = "email_authentication"
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translateResourceKey(
            TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale) + " $code"
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "code" to code
            )))
            .build()
            .mapError { ex -> SendEmailAuthenticationException.Template("Failed to create template for email authentication: ${ex.message}", ex)    }
            .bind()

        emailService.sendEmail(user.email, subject, content, actualLocale)
            .mapError { SendEmailAuthenticationException.from(it) }
            .bind()

        startCooldown(user.email)
            .mapError { ex -> SendEmailAuthenticationException.PostCommitSideEffect("Failed to start cooldown: ${ex.message}", ex) }
            .bind()
    }

    private suspend fun doValidateCode(user: User, code: String): Result<Unit, ValidateEmailTwoFactorCodeException> {
        val details = user.sensitive.security.twoFactor.email

        if (details.expiresAt.isBefore(Instant.now())) {
            return Err(ValidateEmailTwoFactorCodeException.Expired("The code is expired. Please request a new email."))
        }
        if (details.code != code) {
            return Err(ValidateEmailTwoFactorCodeException.WrongCode("The code is invalid."))
        }

        return Ok(Unit)
    }

    /**
     * Enables email as a two-factor authentication (2FA) method for a user.
     *
     * This method ensures the user meets the prerequisites for enabling email-based 2FA,
     * such as having a password and the method not already being enabled. It validates
     * the provided two-factor authentication code, updates the user's configuration to
     * enable email 2FA, saves the updated user information, invalidates any existing
     * access tokens, and optionally sends a security alert if configured.
     *
     * @param req the request containing the code necessary to enable email as a 2FA method
     * @param user the user object for whom email 2FA is to be enabled
     * @param locale an optional locale for localized operations, such as sending security alerts
     * @return a [Result] containing the updated [User] object if successful, or
     *   an [EnableEmailAuthenticationException] if an error occurs during the process
     */
    suspend fun enable(
        req: EnableEmailTwoFactorMethodRequest,
        user: User,
        locale: Locale?
    ): Result<User, EnableEmailAuthenticationException> = coroutineBinding {
        logger.debug { "Enabling email as 2FA method" }

        if (user.sensitive.identities.password == null) {
            Err(EnableEmailAuthenticationException.NoPasswordSet("Cannot enable email as 2FA method: user did not set up authentication using a password."))
                .bind()
        }

        if (user.sensitive.security.twoFactor.email.enabled) {
            Err(EnableEmailAuthenticationException.AlreadyEnabled("Cannot enable email as 2FA method: method is already enabled."))
                .bind()
        }

        doValidateCode(user, req.code)
            .mapError { ex -> EnableEmailAuthenticationException.fromValidateEmailTwoFactorCode(ex) }
            .bind()

        user.sensitive.security.twoFactor.email.code = Random.generateInt().getOrThrow()
        user.sensitive.security.twoFactor.email.expiresAt = Instant.now().plusSeconds(twoFactorEmailCodeProperties.expiresIn)
        user.sensitive.security.twoFactor.email.enabled = true

        if (user.twoFactorMethods.size == 1) {
            user.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL
        }

        val savedUser = userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> EnableEmailAuthenticationException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> EnableEmailAuthenticationException.Database("Failed to save user: ${ex.message}", ex)
            } }
            .bind()

        user.id
            .andThen { userId ->
                accessTokenCache.invalidateAllTokens(userId)
                    .mapError { ex -> EnableEmailAuthenticationException.PostCommitSideEffect("Failed to invalidate all tokens: ${ex.message}", ex) }
            }
            .mapError { ex -> EnableEmailAuthenticationException.PostCommitSideEffect("Failed to invalidate all tokens: ${ex.message}", ex) }
            .bind()

        if (securityAlertProperties.twoFactorAdded  && emailProperties.enable) {
            securityAlertService.sendTwoFactorAdded(
                user,
                twoFactorMethod = TwoFactorMethod.EMAIL,
                locale
            )
                .mapError { ex -> EnableEmailAuthenticationException.PostCommitSideEffect("Failed to send security alert: ${ex.message}", ex) }
                .bind()
        }

        savedUser
    }

    /**
     * Disables email as a two-factor authentication (2FA) method for a given user.
     *
     * @param user The user for whom the email 2FA method is being disabled.
     * @param locale The preferred locale used to send notifications, if applicable. Can be null.
     * @return A [Result] object containing the updated [User] if the operation succeeds,
     *  or a [DisableEmailAuthenticationException] if an error occurs during the process.
     */
    suspend fun disable(
        user: User,
        locale: Locale?
    ): Result<User, DisableEmailAuthenticationException> = coroutineBinding {
        logger.debug { "Disabling email as 2FA method" }

        if (!user.sensitive.security.twoFactor.email.enabled) {
            Err(DisableEmailAuthenticationException.AlreadyDisabled("Cannot disable email as 2FA method: method is already disabled."))
                .bind()
        }

        if (user.twoFactorMethods.size == 1 && user.sensitive.security.twoFactor.email.enabled) {
            Err(DisableEmailAuthenticationException.CannotDisableOnlyTwoFactorMethod("Cannot disable email as 2FA method: it is the only configured 2FA method."))
                .bind()
        }

        user.sensitive.security.twoFactor.email.enabled = false

        val savedUser = userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> DisableEmailAuthenticationException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> DisableEmailAuthenticationException.Database("Failed to save user: ${ex.message}", ex)
            }}
            .bind()

        if (securityAlertProperties.twoFactorRemoved && emailProperties.enable) {
            securityAlertService.sendTwoFactorRemoved(
                user,
                twoFactorMethod = TwoFactorMethod.EMAIL,
                locale,
            )
                .mapError { ex -> DisableEmailAuthenticationException.PostCommitSideEffect("Failed to send security alert: ${ex.message}", ex) }
                .bind()
        }

        savedUser
    }

}
