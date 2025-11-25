package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.exception.model.TwoFactorMethodDisabledException
import io.stereov.singularity.auth.core.exception.model.WrongIdentityProviderException
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.exception.model.CannotDisableOnly2FAMethodException
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorCodeException
import io.stereov.singularity.auth.twofactor.exception.model.TwoFactorCodeExpiredException
import io.stereov.singularity.auth.twofactor.exception.model.TwoFactorMethodAlreadyEnabledException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
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
import io.stereov.singularity.user.core.model.AccountDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class EmailAuthenticationService(
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val userService: UserService,
    private val translateService: TranslateService,
    private val templateService: TemplateService,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val emailService: EmailService,
    private val emailProperties: EmailProperties,
    private val authorizationService: AuthorizationService,
    private val accessTokenCache: AccessTokenCache,
    private val appProperties: AppProperties,
    private val securityAlertProperties: SecurityAlertProperties,
    private val securityAlertService: SecurityAlertService
) {

    private val logger = KotlinLogging.logger {}

    suspend fun sendMail(user: AccountDocument, locale: Locale?) {
        logger.debug { "Generating new code and sending email" }

        val code = Random.generateInt()
        user.sensitive.security.twoFactor.email.code = code
        user.sensitive.security.twoFactor.email.expiresAt = Instant.now().plusSeconds(twoFactorEmailCodeProperties.expiresIn)

        userService.save(user)

        sendAuthenticationEmail(user, code, locale)
    }

    suspend fun validateCode(user: AccountDocument, code: String): AccountDocument {
        logger.debug { "Validating 2FA code" }

        if (!user.sensitive.security.twoFactor.email.enabled)
            throw TwoFactorMethodDisabledException(TwoFactorMethod.EMAIL)

        doValidateCode(user, code)

        return userService.save(user)
    }

    suspend fun sendAuthenticationEmail(user: AccountDocument, code: String, locale: Locale?) {
        logger.debug { "Sending verification email to ${user.sensitive.email}" }

        val userId = user.id
        val actualLocale = locale ?: appProperties.locale

        if (!user.sensitive.identities.contains(IdentityProvider.PASSWORD))
            throw WrongIdentityProviderException("Cannot enable email as 2FA method: password authentication required")

        val remainingCooldown = getRemainingCooldown(userId)
        if (remainingCooldown > 0) {
            throw EmailCooldownException(remainingCooldown)
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

        emailService.sendEmail(user.requireNotGuestAndGetEmail(), subject, content, actualLocale)
        startCooldown(userId)
    }

    suspend fun getRemainingCooldown(userId: ObjectId): Long {
        logger.debug { "Getting remaining cooldown for email authentication" }

        val key = "email-authentication-cooldown:$userId"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    private suspend fun doValidateCode(user: AccountDocument, code: String) {
        val details = user.sensitive.security.twoFactor.email

        if (details.expiresAt.isBefore(Instant.now()))
            throw TwoFactorCodeExpiredException("The code is expired. Please request a new email.")
        if (details.code != code)
            throw InvalidTwoFactorCodeException("Wrong code.")

        details.code = Random.generateInt()
        details.expiresAt = Instant.now().plusSeconds(twoFactorEmailCodeProperties.expiresIn)
    }

    private suspend fun startCooldown(userId: ObjectId): Boolean {
        logger.debug { "Starting cooldown for email authentication" }

        val key = "email-authentication-cooldown:$userId"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(emailProperties.sendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }

    suspend fun enable(req: EnableEmailTwoFactorMethodRequest, locale: Locale?): AccountDocument {
        logger.debug { "Enabling email as 2FA method" }

        val user = authorizationService.getUser()
        authorizationService.requireStepUp()

        if (!user.sensitive.identities.contains(IdentityProvider.PASSWORD))
            throw WrongIdentityProviderException("Cannot enable email as 2FA method: password authentication required")

        if (user.sensitive.security.twoFactor.email.enabled)
            throw TwoFactorMethodAlreadyEnabledException("Mail is already enabled as 2FA method")

        doValidateCode(user, req.code)

        user.sensitive.security.twoFactor.email.enabled = true

        if (user.twoFactorMethods.size == 1)
            user.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL

        val savedUser = userService.save(user)
        accessTokenCache.invalidateAllTokens(user.id)

        if (securityAlertProperties.twoFactorAdded  && emailProperties.enable) {
            securityAlertService.send(
                user,
                locale,
                SecurityAlertType.TWO_FACTOR_ADDED,
                twoFactorMethod = TwoFactorMethod.EMAIL
            )
        }

        return savedUser
    }

    suspend fun disable(locale: Locale?): AccountDocument {
        logger.debug { "Disabling email as 2FA method" }

        val user = authorizationService.getUser()
        authorizationService.requireStepUp()

        if (!user.sensitive.security.twoFactor.email.enabled)
            throw TwoFactorMethodDisabledException(TwoFactorMethod.EMAIL)

        if (user.twoFactorMethods.size == 1 && user.sensitive.security.twoFactor.email.enabled)
            throw CannotDisableOnly2FAMethodException("Failed to disable email: it not allowed to disable the only configured 2FA method.")

        user.sensitive.security.twoFactor.email.enabled = false

        val savedUser = userService.save(user)
        if (securityAlertProperties.twoFactorRemoved && emailProperties.enable) {
            securityAlertService.send(
                user,
                locale,
                SecurityAlertType.TWO_FACTOR_REMOVED,
                twoFactorMethod = TwoFactorMethod.EMAIL
            )
        }

        return savedUser
    }

}
