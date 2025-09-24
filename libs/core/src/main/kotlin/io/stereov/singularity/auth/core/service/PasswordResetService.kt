package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.WrongIdentityProviderException
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.properties.PasswordResetProperties
import io.stereov.singularity.auth.core.service.token.PasswordResetTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.exception.model.EmailCooldownException
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.exception.model.UserDoesNotExistException
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
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
    private val passwordResetProperties: PasswordResetProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val accessTokenCache: AccessTokenCache,
    private val appProperties: AppProperties
) {

    private val logger = KotlinLogging.logger {}

    suspend fun sendPasswordReset(req: SendPasswordResetRequest, locale: Locale?) {
        logger.debug { "Sending password reset email" }

        try {
            val user = userService.findByEmail(req.email)
            return sendPasswordResetEmail(user, locale)
        } catch (_: UserDoesNotExistException) {
            startCooldown(req.email)
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
        val passwordIdentity = user.sensitive.identities[IdentityProvider.PASSWORD]
            ?: throw WrongIdentityProviderException("No password authentication is set for user")

        passwordIdentity.password = hashService.hashBcrypt(req.newPassword)
        user.clearSessions()
        accessTokenCache.invalidateAllTokens(user.id)

        userService.save(user)
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

    private suspend fun sendPasswordResetEmail(user: UserDocument, locale: Locale?) {
        logger.debug { "Sending password reset email to ${user.sensitive.email}" }

        val email = user.requireNotGuestAndGetEmail()
        val actualLocale = locale ?: appProperties.locale

        val remainingCooldown = getRemainingCooldown(email)
        if (remainingCooldown > 0) {
            throw EmailCooldownException(remainingCooldown)
        }

        val secret = user.sensitive.security.password.resetSecret

        val token = passwordResetTokenService.create(user.id, secret)
        val passwordResetUrl = generatePasswordResetUrl(token)

        val slug = "password_reset"
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"), EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "reset_url" to passwordResetUrl
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
        startCooldown(email)
    }

    private fun generatePasswordResetUrl(token: String): String {
        return "${passwordResetProperties.uri}?token=$token"
    }
}
