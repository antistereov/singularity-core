package io.stereov.web.config

import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.mail.MailService
import io.stereov.web.global.service.mail.MailTokenService
import io.stereov.web.global.service.mail.MailVerificationCooldownService
import io.stereov.web.properties.MailProperties
import io.stereov.web.properties.UiProperties
import io.stereov.web.user.controller.UserMailVerificationController
import io.stereov.web.user.service.UserMailVerificationService
import io.stereov.web.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
 * - [MailVerificationCooldownService]
 * - [MailTokenService]
 *
 * It enables the following controllers:
 * - [UserMailVerificationController]
 *
 * It enables the following beans:
 * - [JavaMailSender]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@ConditionalOnProperty(prefix = "baseline.mail", name = ["enable-verification"], havingValue = "true", matchIfMissing = false)
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
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

    @Bean
    @ConditionalOnMissingBean
    fun mailService(
        mailSender: JavaMailSender,
        mailProperties: MailProperties,
        uiProperties: UiProperties,
        mailVerificationCooldownService: MailVerificationCooldownService,
        mailTokenService: MailTokenService,
    ): MailService {
        return MailService(mailSender, mailProperties, uiProperties, mailVerificationCooldownService, mailTokenService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailVerificationCooldownService(
        redisTemplate: ReactiveRedisTemplate<String, String>,
        mailProperties: MailProperties
    ): MailVerificationCooldownService {
        return MailVerificationCooldownService(redisTemplate, mailProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailTokenService(
        mailProperties: MailProperties,
        jwtService: JwtService
    ): MailTokenService {
        return MailTokenService(mailProperties, jwtService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userMailVerificationService(
        userService: UserService,
        authenticationService: AuthenticationService,
        mailVerificationCooldownService: MailVerificationCooldownService,
        mailService: MailService,
        mailTokenService: MailTokenService,
    ): UserMailVerificationService {
        return UserMailVerificationService(userService, authenticationService, mailVerificationCooldownService, mailService, mailTokenService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userMailController(
        userMailVerificationService: UserMailVerificationService
    ): UserMailVerificationController {
        return UserMailVerificationController(userMailVerificationService)
    }
}
