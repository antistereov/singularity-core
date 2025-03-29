package io.stereov.web.test.config

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender

@TestConfiguration
class MockMailSenderConfig {
    @Bean
    fun mailSender(): JavaMailSender = mockk(relaxed = true)
}
