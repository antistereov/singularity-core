package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.NoAccountInfoService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.exception.ResetPasswordException
import io.stereov.singularity.auth.core.exception.SendPasswordResetException
import io.stereov.singularity.auth.core.model.NoAccountInfoAction
import io.stereov.singularity.auth.token.model.PasswordResetToken
import io.stereov.singularity.auth.token.service.PasswordResetTokenService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.database.core.exception.DocumentException
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.service.HashService
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
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.principal.core.exception.FindUserByEmailException
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.identity.UserIdentity
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Service
import java.util.*

@Service
class PasswordResetService(
    private val userService: UserService,
    private val passwordResetTokenService: PasswordResetTokenService,
    private val hashService: HashService,
    override val cacheService: CacheService,
    override val emailProperties: EmailProperties,
    private val uiProperties: UiProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val appProperties: AppProperties,
    private val securityAlertService: SecurityAlertService,
    private val securityAlertProperties: SecurityAlertProperties,
    private val noAccountInfoService: NoAccountInfoService,
    private val accessTokenCache: AccessTokenCache
) : CooldownEmailService {

    override val logger = KotlinLogging.logger {}
    override val slug = "password_reset"

    suspend fun sendPasswordReset(
        req: SendPasswordResetRequest,
        locale: Locale?
    ): Result<Long, SendPasswordResetException> = coroutineBinding {
        logger.debug { "Sending password reset email" }

        val remainingCooldown = getRemainingCooldown(req.email)
            .mapError { ex -> SendPasswordResetException.CooldownCache("Failed to retrieve cooldown: ${ex.message}", ex) }
            .bind()

        if (remainingCooldown.seconds > 0) {
            Err(SendPasswordResetException.CooldownActive("Cooldown is still active for password reset"))
                .bind()
        }

        userService.findByEmail(req.email)
            .flatMapEither(
                success = { user -> sendPasswordResetEmail(user, locale) },
                failure = { ex ->
                    when (ex) {
                        is FindUserByEmailException.UserNotFound -> {
                            noAccountInfoService.send(req.email, NoAccountInfoAction.PASSWORD_RESET, locale)
                                .onFailure { ex -> logger.error(ex) { "Failed to send no account info email"} }
                            startCooldown(req.email)
                                .mapError { ex -> SendPasswordResetException.CooldownCache("Failed to start cooldown: ${ex.message}", ex) }
                        }
                        else -> Err(SendPasswordResetException.Database("Failed to find user by email: ${ex.message}", ex))
                    } }
            )
            .bind()
    }

    suspend fun resetPassword(
        token: PasswordResetToken,
        req: ResetPasswordRequest, locale: Locale?
    ): Result<Unit, ResetPasswordException> = coroutineBinding {
        logger.debug { "Resetting password "}

        val user = userService.findById(token.userId)
            .mapError { ex -> when (ex) {
                is FindEncryptedDocumentByIdException.NotFound -> ResetPasswordException.UserNotFound("User not found")
                else -> ResetPasswordException.Database("Failed to find user by id: ${ex.message}", ex)
            } }
            .bind()

        val savedSecret = user.sensitive.security.password.resetSecret

        val tokenIsValid = token.secret == savedSecret

        if (!tokenIsValid) {
            Err(ResetPasswordException.InvalidToken("The provided token does not match the user's reset secret"))
                .bind()
        }

        user.sensitive.security.password.resetSecret = Random.generateString(20).getOrThrow()
        val passwordIdentity = user.sensitive.identities.password
        val newPasswordHash = hashService.hashBcrypt(req.newPassword)
            .mapError { ex -> ResetPasswordException.Hash("Failed to hash new password: ${ex.message}", ex) }
            .bind()

        if (passwordIdentity != null) {
            passwordIdentity.password = newPasswordHash
        } else {
            user.sensitive.identities.password = UserIdentity.ofPassword(newPasswordHash)
        }
        user.clearSessions()

        val updatedUser = userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> ResetPasswordException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> ResetPasswordException.Database("Failed to save updated user: ${ex.message}", ex)
            } }
            .bind()

        if (emailProperties.enable && securityAlertProperties.passwordChanged) {
            securityAlertService.sendPasswordChanged(updatedUser, locale)
                .mapError { ex -> ResetPasswordException.PostCommitSideEffect("Failed to send password changed alert: ${ex.message}", ex) }
                .bind()
        }

        accessTokenCache.invalidateAllTokens(token.userId)
            .mapError { ex -> ResetPasswordException.PostCommitSideEffect("Failed to invalidate all tokens: ${ex.message}", ex) }
            .bind()
    }

    private suspend fun sendPasswordResetEmail(
        user: User,
        locale: Locale?
    ): Result<Long, SendPasswordResetException> = coroutineBinding {
        logger.debug { "Sending password reset email to ${user.sensitive.email}" }

        val email = user.email
        val actualLocale = locale ?: appProperties.locale

        val passwordResetUri = generatePasswordResetUri(user)
            .mapError { ex -> SendPasswordResetException.Token("Failed to generate password reset URI: ${ex.message}", ex) }
            .bind()

        val slug = "password_reset"
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"), EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "reset_uri" to passwordResetUri
            )))
            .build()
            .mapError { ex -> SendPasswordResetException.Template("Failed to create template for password reset: ${ex.message}", ex) }
            .bind()

        emailService.sendEmail(email, subject, content, actualLocale)
            .mapError { SendPasswordResetException.from(it) }
            .bind()

        startCooldown(email)
            .mapError { ex -> SendPasswordResetException.CooldownCache("Failed to start cooldown: ${ex.message}", ex) }
            .bind()
    }

    suspend fun generatePasswordResetUri(user: User): Result<String, DocumentException.Invalid> = coroutineBinding {
        val userId = user.id.bind()
        val secret = user.sensitive.security.password.resetSecret
        val token = passwordResetTokenService.create(userId, secret)
        "${uiProperties.passwordResetUri}?token=$token"
    }
}
