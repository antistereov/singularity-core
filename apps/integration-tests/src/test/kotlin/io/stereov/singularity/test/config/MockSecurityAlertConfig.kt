package io.stereov.singularity.test.config

import io.mockk.mockk
import io.stereov.singularity.auth.core.service.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockSecurityAlertConfig {

    @Bean
    fun loginAlertService(): LoginAlertService = mockk(relaxed = true)

    @Bean
    fun securityAlertService(): SecurityAlertService = mockk(relaxed = true)

    @Bean
    fun registrationAlertService(): RegistrationAlertService = mockk(relaxed = true)

    @Bean
    fun noAccountInfoService(): NoAccountInfoService = mockk(relaxed = true)

    @Bean
    fun identityProviderInfoService(): IdentityProviderInfoService = mockk(relaxed = true)
}
