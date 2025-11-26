package io.stereov.singularity.auth.alert.config

import io.stereov.singularity.auth.alert.component.ProviderStringCreator
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.*
import io.stereov.singularity.auth.core.service.PasswordResetService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
@EnableConfigurationProperties(
    SecurityAlertProperties::class
)
class AlertConfiguration {

    // Component

    @Bean
    @ConditionalOnMissingBean
    fun providerStringCreator(translateService: TranslateService) = ProviderStringCreator(translateService)


    // Services
    
    @Bean
    @ConditionalOnMissingBean
    fun identityProviderInfoService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        providerStringCreator: ProviderStringCreator,
        emailProperties: EmailProperties,
        passwordResetService: PasswordResetService,
        cacheService: CacheService
    ) = IdentityProviderInfoService(
        appProperties,
        translateService,
        emailService,
        templateService,
        providerStringCreator,
        passwordResetService,
        cacheService,
        emailProperties,
    )

    @Bean
    @ConditionalOnMissingBean
    fun loginAlertService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService
    ) = LoginAlertService(
        appProperties,
        translateService,
        emailService,
        templateService
    )

    @Bean
    @ConditionalOnMissingBean
    fun noAccountInfoService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        cacheService: CacheService,
        emailProperties: EmailProperties
    ) = NoAccountInfoService(
        appProperties,
        translateService,
        emailService,
        templateService,
        cacheService,
        emailProperties
    )

    @Bean
    @ConditionalOnMissingBean
    fun registrationAlertService(
        translateService: TranslateService,
        templateService: TemplateService,
        appProperties: AppProperties,
        emailService: EmailService,
        providerStringCreator: ProviderStringCreator
    ) = RegistrationAlertService(
        translateService,
        templateService,
        appProperties,
        emailService,
        providerStringCreator
    )

    @Bean
    @ConditionalOnMissingBean
    fun securityAlertService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService
    ) = SecurityAlertService(
        appProperties,
        translateService,
        emailService,
        templateService
    )
}
