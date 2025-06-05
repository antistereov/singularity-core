package io.stereov.singularity.mail.config

import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.mail.exception.handler.MailExceptionHandler
import io.stereov.singularity.mail.properties.MailProperties
import io.stereov.singularity.mail.service.MailService
import io.stereov.singularity.mail.service.MailTemplateService
import io.stereov.singularity.template.service.TemplateService
import io.stereov.singularity.user.controller.UserMailController
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.mail.MailCooldownService
import io.stereov.singularity.user.service.mail.MailTokenService
import io.stereov.singularity.user.service.mail.UserMailSender
import io.stereov.singularity.user.service.mail.UserMailService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

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
        mailTemplateService: MailTemplateService
    ): MailService {
        return MailService(mailSender, mailProperties, uiProperties, mailCooldownService, mailTokenService, mailTemplateService)
    }


    @Bean
    @ConditionalOnMissingBean
    fun userMailService(
        userService: UserService,
        authenticationService: AuthenticationService,
        mailCooldownService: MailCooldownService,
        mailService: UserMailSender,
        mailTokenService: MailTokenService,
        hashService: HashService,
    ): UserMailService {
        return UserMailService(userService, authenticationService, mailCooldownService, mailService, mailTokenService, hashService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun mainTemplateService(templateService: TemplateService) = MailTemplateService(templateService)

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
