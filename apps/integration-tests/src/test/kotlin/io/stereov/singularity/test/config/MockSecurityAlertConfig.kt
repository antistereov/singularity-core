package io.stereov.singularity.test.config

import io.mockk.mockk
import io.mockk.spyk
import io.stereov.singularity.auth.alert.service.*
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockSecurityAlertConfig {

    @Autowired
    private lateinit var appProperties: AppProperties
    @Autowired
    private lateinit var translateService: TranslateService
    @Autowired
    private lateinit var emailService: EmailService
    @Autowired
    private lateinit var templateService: TemplateService


    @Bean
    fun loginAlertService(): LoginAlertService = mockk(relaxed = true)

    @Bean
    fun securityAlertService(): SecurityAlertService = spyk(SecurityAlertService(appProperties, translateService, emailService, templateService))

    @Bean
    fun registrationAlertService(): RegistrationAlertService = mockk(relaxed = true)

    @Bean
    fun noAccountInfoService(): NoAccountInfoService = mockk(relaxed = true)

    @Bean
    fun identityProviderInfoService(): IdentityProviderInfoService = mockk(relaxed = true)
}
