package io.stereov.singularity.config

import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.global.service.encryption.service.EncryptionService
import io.stereov.singularity.global.service.hash.HashService
import io.stereov.singularity.global.service.jwt.JwtService
import io.stereov.singularity.global.service.mail.MailCooldownService
import io.stereov.singularity.global.service.mail.MailService
import io.stereov.singularity.global.service.mail.MailTokenService
import io.stereov.singularity.global.service.mail.exception.handler.MailExceptionHandler
import io.stereov.singularity.properties.MailProperties
import io.stereov.singularity.properties.UiProperties
import io.stereov.singularity.user.controller.UserMailController
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.mail.UserMailService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

/**
 * # Configuration class for mail-related beans.
 *
 * This class is responsible for configuring the mail-related services
 * and components in the application.
 *
 * It is configured only if the property `baseline.mail.enable-verification` is set to `true`.
 *
 * It runs after the [MongoReactiveAutoConfiguration], [SpringDataWebAutoConfiguration],
 * [RedisAutoConfiguration], and [ApplicationConfiguration] classes to ensure that
 * the necessary configurations are applied in the correct order.
 *
 * It enables the following configuration properties:
 * - [MailProperties]
 *
 * This class enables the following services:
 * - [MailService]
 * - [MailCooldownService]
 * - [MailTokenService]
 *
 * It enables the following controllers:
 * - [UserMailController]
 *
 * It enables the following beans:
 * - [JavaMailSender]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(MailProperties::class)
class MailConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun javaMailSender(mailProperties: MailProperties): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = mailProperties.host
        mailSender.port = mailProperties.port
        mailSender.username = mailProperties.username
        mailSender.password = mailProperties.password

        val props = mailSender.javaMailProperties
        props["mail.transport.protocol"] = mailProperties.transportProtocol
        props["mail.smtp.auth"] = mailProperties.smtpAuth
        props["mail.smtp.starttls.enable"] = mailProperties.smtpStarttls
        props["mail.debug"] = mailProperties.debug
        return mailSender
    }

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun mailService(
        mailSender: JavaMailSender,
        mailProperties: MailProperties,
        uiProperties: UiProperties,
        mailCooldownService: MailCooldownService,
        mailTokenService: MailTokenService,
    ): MailService {
        return MailService(mailSender, mailProperties, uiProperties, mailCooldownService, mailTokenService)
    }

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
        mailService: MailService,
        mailTokenService: MailTokenService,
        hashService: HashService,
    ): UserMailService {
        return UserMailService(userService, authenticationService, mailCooldownService, mailService, mailTokenService, hashService)
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userMailController(
        userMailService: UserMailService
    ): UserMailController {
        return UserMailController(userMailService)
    }

    // ExceptionHandler

    @Bean
    @ConditionalOnMissingBean
    fun mailExceptionHandler(): MailExceptionHandler {
        return MailExceptionHandler()
    }
}
