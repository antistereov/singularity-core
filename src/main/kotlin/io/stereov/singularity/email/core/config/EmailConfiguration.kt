package io.stereov.singularity.email.core.config

import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.service.EmailTemplateService
import io.stereov.singularity.email.core.service.EnabledEmailService
import io.stereov.singularity.email.core.service.FailingEmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
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
@EnableConfigurationProperties(EmailProperties::class)
class EmailConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "singularity.email", value = ["enable"], havingValue = "true", matchIfMissing = false)
    fun javaMailSender(emailProperties: EmailProperties): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = emailProperties.host
        mailSender.port = emailProperties.port
        mailSender.username = emailProperties.username
        mailSender.password = emailProperties.password

        val props = mailSender.javaMailProperties
        props["mail.transport.protocol"] = emailProperties.transportProtocol
        props["mail.smtp.auth"] = emailProperties.smtpAuth
        props["mail.smtp.starttls.enable"] = emailProperties.smtpStarttls
        props["mail.debug"] = emailProperties.debug
        return mailSender
    }

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun mailService(
        mailSender: JavaMailSender,
        emailProperties: EmailProperties,
        emailTemplateService: EmailTemplateService,
        appProperties: AppProperties
    ): EmailService {
        return if (emailProperties.enable) {
            EnabledEmailService(mailSender, emailProperties, emailTemplateService, appProperties)
        } else {
            FailingEmailService()
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailTemplateService(templateService: TemplateService) = EmailTemplateService(templateService)
}
