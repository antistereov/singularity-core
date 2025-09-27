package io.stereov.singularity.test

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.stereov.singularity.test.config.MockMailSenderConfig
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@Import(MockMailSenderConfig::class)
class BaseMailIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var mailSender: JavaMailSender

    @BeforeEach
    fun init() {
        clearMocks(mailSender)
        every { mailSender.send(any<MimeMessage>()) } just Runs
    }

    companion object {

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.email.enable") { true }
        }
    }
}
