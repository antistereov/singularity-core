package io.stereov.singularity.test.config

import io.mockk.mockk
import io.stereov.singularity.auth.core.service.LoginAlertService
import io.stereov.singularity.auth.core.service.SecurityAlertService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockSecurityAlertConfig {

    @Bean
    fun loginAlertService(): LoginAlertService = mockk(relaxed = true)

    @Bean
    fun securityAlertService(): SecurityAlertService = mockk(relaxed = true)
}