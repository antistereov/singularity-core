package io.stereov.singularity.user.mail.config

import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.jwt.service.JwtService
import io.stereov.singularity.mail.properties.MailProperties
import io.stereov.singularity.mail.service.MailService
import io.stereov.singularity.template.service.TemplateService
import io.stereov.singularity.user.core.config.UserConfiguration
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.mail.controller.UserMailController
import io.stereov.singularity.user.mail.service.MailCooldownService
import io.stereov.singularity.user.mail.service.MailTokenService
import io.stereov.singularity.user.mail.service.UserMailSender
import io.stereov.singularity.user.mail.service.UserMailService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveRedisTemplate

@Configuration
@AutoConfiguration(
    after = [
        UserConfiguration::class
    ]
)
class UserMailConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userMailController(
        userMailService: UserMailService
    ): UserMailController {
        return UserMailController(userMailService)
    }

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun mailCooldownService(
        redisTemplate: ReactiveRedisTemplate<String, String>,
        mailProperties: MailProperties
    ): MailCooldownService {
        return MailCooldownService(redisTemplate, mailProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailTokenService(
        mailProperties: MailProperties,
        jwtService: JwtService,
        encryptionService: EncryptionService
    ): MailTokenService {
        return MailTokenService(mailProperties, jwtService, encryptionService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userMailService(
        userService: UserService,
        authenticationService: AuthenticationService,
        mailCooldownService: MailCooldownService,
        mailService: UserMailSender,
        mailTokenService: MailTokenService,
        hashService: HashService
    ) = UserMailService(userService, authenticationService, mailCooldownService, mailService, mailTokenService, hashService)

    @Bean
    @ConditionalOnMissingBean
    fun userMailSender(
        mailCooldownService: MailCooldownService,
        mailTokenService: MailTokenService,
        uiProperties: UiProperties,
        translateService: TranslateService,
        mailService: MailService,
        templateService: TemplateService
    ) = UserMailSender(mailCooldownService, mailTokenService, uiProperties, translateService, mailService, templateService)

}
