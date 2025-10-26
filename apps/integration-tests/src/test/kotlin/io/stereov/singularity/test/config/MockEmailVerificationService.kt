package io.stereov.singularity.test.config

import io.mockk.mockk
import io.stereov.singularity.auth.core.service.EmailVerificationService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockEmailVerificationService {

    @Bean
    fun emailVerificationService() = mockk<EmailVerificationService>(relaxed = true)
}