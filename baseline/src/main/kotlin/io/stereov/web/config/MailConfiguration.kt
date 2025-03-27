package io.stereov.web.config

import io.stereov.web.global.service.mail.MailService
import io.stereov.web.global.service.mail.MailTokenService
import io.stereov.web.global.service.mail.MailVerificationCooldownService
import io.stereov.web.properties.*
import org.springframework.boot.autoconfigure.AutoConfiguration
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
    fun mailService(
        mailSender: JavaMailSender,
        mailProperties: MailProperties,
        frontendProperties: FrontendProperties,
        mailVerificationCooldownService: MailVerificationCooldownService,
        mailTokenService: MailTokenService,
    ): MailService {
        return MailService(mailSender, mailProperties, frontendProperties, mailVerificationCooldownService, mailTokenService)
    }

    @Bean
    fun mailVerificationCooldownService(
        redisTemplate: ReactiveRedisTemplate<String, String>,
        mailProperties: MailProperties
    ): MailVerificationCooldownService {
        return MailVerificationCooldownService(redisTemplate, mailProperties)
    }
}
