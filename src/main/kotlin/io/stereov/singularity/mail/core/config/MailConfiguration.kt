package io.stereov.singularity.mail.core.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.mail.core.exception.handler.MailExceptionHandler
import io.stereov.singularity.mail.core.properties.MailProperties
import io.stereov.singularity.mail.core.service.EnabledMailService
import io.stereov.singularity.mail.core.service.FailingMailService
import io.stereov.singularity.mail.core.service.MailService
import io.stereov.singularity.mail.core.service.MailTemplateService
import io.stereov.singularity.mail.template.service.TemplateService
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
            EnabledMailService(mailSender, mailProperties, mailTemplateService)
        } else {
            FailingMailService()
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailTemplateService(templateService: TemplateService) = MailTemplateService(templateService)

    // ExceptionHandler

    @Bean
    @ConditionalOnMissingBean
    fun mailExceptionHandler(): MailExceptionHandler {
        return MailExceptionHandler()
    }
}
