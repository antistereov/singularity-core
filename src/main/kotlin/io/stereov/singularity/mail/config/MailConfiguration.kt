package io.stereov.singularity.mail.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.mail.exception.handler.MailExceptionHandler
import io.stereov.singularity.mail.properties.MailProperties
import io.stereov.singularity.mail.service.MailDisabledService
import io.stereov.singularity.mail.service.MailService
import io.stereov.singularity.mail.service.MailServiceImpl
import io.stereov.singularity.mail.service.MailTemplateService
import io.stereov.singularity.template.service.TemplateService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
class MailConfiguration(
    private val appProperties: AppProperties
) {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "singularity.app", value = ["enable-mail"], havingValue = "true", matchIfMissing = false)
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
        mailTemplateService: MailTemplateService
    ): MailService {
        return if (appProperties.enableMail) {
            MailServiceImpl(mailSender, mailProperties, mailTemplateService)
        } else {
            MailDisabledService()
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun mainTemplateService(templateService: TemplateService) = MailTemplateService(templateService)

    // ExceptionHandler

    @Bean
    @ConditionalOnMissingBean
    fun mailExceptionHandler(): MailExceptionHandler {
        return MailExceptionHandler()
    }
}
