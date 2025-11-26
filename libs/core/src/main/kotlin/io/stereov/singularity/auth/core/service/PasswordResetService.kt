package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.service.NoAccountInfoService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.model.NoAccountInfoAction
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.token.service.PasswordResetTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.exception.model.EmailCooldownException
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.principal.core.exception.model.UserDoesNotExistException
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.identity.UserIdentity
import io.stereov.singularity.principal.core.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class PasswordResetService(
    private val userService: UserService,
    private val passwordResetTokenService: PasswordResetTokenService,
    private val hashService: HashService,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val emailProperties: EmailProperties,
    private val uiProperties: UiProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val accessTokenCache: AccessTokenCache,
    private val appProperties: AppProperties,
    private val securityAlertService: SecurityAlertService,
    private val securityAlertProperties: SecurityAlertProperties,
    private val noAccountInfoService: NoAccountInfoService
) {

    private val logger = KotlinLogging.logger {}

    suspend fun sendPasswordReset(req: SendPasswordResetRequest, locale: Locale?) {
        logger.debug { "Sending password reset email" }

        val remainingCooldown = getRemainingCooldown(req.email)
        if (remainingCooldown > 0) {
            throw EmailCooldownException(remainingCooldown)
        }

        try {
            val user = userService.findByEmail(req.email)
            return sendPasswordResetEmail(user, locale)
        } catch (_: UserDoesNotExistException) {
            noAccountInfoService.send(req.email, NoAccountInfoAction.PASSWORD_RESET, locale)
            startCooldown(req.email)
            return
        }
    }

    suspend fun resetPassword(token: String, req: ResetPasswordRequest, locale: Locale?) {
        logger.debug { "Resetting password "}

        val resetToken = passwordResetTokenService.extract(token)
        val user = userService.findById(resetToken.userId)

        val savedSecret = user.sensitive.security.password.resetSecret

        val tokenIsValid = resetToken.secret == savedSecret

        if (!tokenIsValid) {
            throw AuthException("Password request secret does not match")
        }

        user.sensitive.security.password.resetSecret = Random.generateString(20)
        val passwordIdentity = user.sensitive.identities[IdentityProvider.PASSWORD]
        val newPasswordHash = hashService.hashBcrypt(req.newPassword)
        if (passwordIdentity != null) {
            passwordIdentity.password = newPasswordHash
        } else {
            user.sensitive.identities[IdentityProvider.PASSWORD] = UserIdentity.ofPassword(newPasswordHash)
        }
        user.clearSessions()
        accessTokenCache.invalidateAllTokens(user.id)

        val updatedUser = userService.save(user)

        if (emailProperties.enable && securityAlertProperties.passwordChanged) {
            securityAlertService.send(updatedUser, locale, SecurityAlertType.PASSWORD_CHANGED)
        }
    }

    suspend fun getRemainingCooldown(req: SendPasswordResetRequest): MailCooldownResponse {
        logger.debug { "Getting remaining password reset cooldown" }

        val remaining = getRemainingCooldown(req.email)

        return MailCooldownResponse(remaining)
    }

    suspend fun startCooldown(email: String): Boolean {
        logger.debug { "Starting cooldown for password reset" }

        val key = "password-reset-cooldown:$email"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(emailProperties.sendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }

    suspend fun getRemainingCooldown(email: String): Long {
        logger.debug { "Getting remaining cooldown for password resets" }

        val key = "password-reset-cooldown:$email"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    private suspend fun sendPasswordResetEmail(user: User, locale: Locale?) {
        logger.debug { "Sending password reset email to ${user.sensitive.email}" }

        val email = user.requireNotGuestAndGetEmail()
        val actualLocale = locale ?: appProperties.locale

        val passwordResetUri = generatePasswordResetUri(user)

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

        emailService.sendEmail(email, subject, content, actualLocale)
        startCooldown(email)
    }

    suspend fun generatePasswordResetUri(user: User): String {
        val secret = user.sensitive.security.password.resetSecret
        val token = passwordResetTokenService.create(user.id, secret)
        return "${uiProperties.passwordResetUri}?token=$token"
    }
}
