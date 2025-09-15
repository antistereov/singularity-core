package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.exception.model.TwoFactorMethodDisabledException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.dto.request.EnableMailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.exception.model.CannotDisableOnly2FAMethodException
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorCodeException
import io.stereov.singularity.auth.twofactor.exception.model.TwoFactorCodeExpiredException
import io.stereov.singularity.auth.twofactor.exception.model.TwoFactorMethodSetupException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.auth.twofactor.properties.TwoFactorMailCodeProperties
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.translate.model.TranslateKey
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.mail.core.exception.model.MailCooldownException
import io.stereov.singularity.mail.core.properties.MailProperties
import io.stereov.singularity.mail.core.service.MailService
import io.stereov.singularity.mail.core.util.MailConstants
import io.stereov.singularity.mail.template.service.TemplateService
import io.stereov.singularity.mail.template.util.TemplateBuilder
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class MailAuthenticationService(
    private val twoFactorMailCodeProperties: TwoFactorMailCodeProperties,
    private val userService: UserService,
    private val translateService: TranslateService,
    private val templateService: TemplateService,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val mailService: MailService,
    private val mailProperties: MailProperties,
    private val authorizationService: AuthorizationService,
    private val accessTokenCache: AccessTokenCache
) {

    private val logger = KotlinLogging.logger {}

    suspend fun sendMail(user: UserDocument, lang: Language) {
        logger.debug { "Generating new code and sending email" }

        val code = Random.generateInt()
        user.sensitive.security.twoFactor.mail.code = code
        user.sensitive.security.twoFactor.mail.expiresAt = Instant.now().plusSeconds(twoFactorMailCodeProperties.expiresIn)

        userService.save(user)

        sendAuthenticationEmail(user, code, lang)
    }

    suspend fun validateCode(user: UserDocument, code: String): UserDocument {
        logger.debug { "Validating 2FA code" }

        if (!user.sensitive.security.twoFactor.mail.enabled)
            throw TwoFactorMethodDisabledException(TwoFactorMethod.MAIL)

        doValidateCode(user, code)

        return user
    }

    suspend fun sendAuthenticationEmail(user: UserDocument, code: String, lang: Language) {
        logger.debug { "Sending verification email to ${user.sensitive.email}" }

        val userId = user.id

        val remainingCooldown = getRemainingCooldown(userId)
        if (remainingCooldown > 0) {
            throw MailCooldownException(remainingCooldown)
        }

        val slug = "email_authentication"
        val templatePath = "${MailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translate(
            TranslateKey("$slug.subject"),
            MailConstants.RESOURCE_BUNDLE, lang) + " $code"
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(MailConstants.RESOURCE_BUNDLE, lang)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "code" to code
            )))
            .build()

        mailService.sendEmail(user.sensitive.email, subject, content, lang)
        startCooldown(userId)
    }

    suspend fun getRemainingCooldown(userId: ObjectId): Long {
        logger.debug { "Getting remaining cooldown for email authentication" }

        val key = "email-verification-cooldown:$userId"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    private fun doValidateCode(user: UserDocument, code: String) {
        val details = user.sensitive.security.twoFactor.mail

        if (details.expiresAt.isAfter(Instant.now()))
            throw TwoFactorCodeExpiredException("The code is expired. Please request a new email.")
        if (details.code != code)
            throw InvalidTwoFactorCodeException("Wrong code.")
    }

    private suspend fun startCooldown(userId: ObjectId): Boolean {
        logger.debug { "Starting cooldown for email authentication" }

        val key = "email-authentication-cooldown:$userId"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(mailProperties.sendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }

    suspend fun enable(req: EnableMailTwoFactorMethodRequest): UserDocument {
        logger.debug { "Enabling mail as 2FA method" }

        val user = authorizationService.getCurrentUser()
        authorizationService.requireStepUp()

        if (user.sensitive.security.twoFactor.mail.enabled)
            throw TwoFactorMethodSetupException("Mail is already enabled as 2FA method")

        doValidateCode(user, req.code)

        user.sensitive.security.twoFactor.mail.enabled = true

        val savedUser = userService.save(user)
        accessTokenCache.invalidateAllTokens(user.id)

        return savedUser
    }

    suspend fun disable(): UserDocument {
        logger.debug { "Disabling mail as 2FA method" }

        val user = authorizationService.getCurrentUser()
        authorizationService.requireStepUp()

        if (user.twoFactorMethods.size == 1 && user.sensitive.security.twoFactor.mail.enabled)
            throw CannotDisableOnly2FAMethodException("Failed to disable mail: it not allowed to disable the only configured 2FA method.")

        user.sensitive.security.twoFactor.mail.enabled = false

        return userService.save(user)
    }

}