package io.stereov.web.test.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockConfig {
    @Bean
    fun googleAuthenticator(): GoogleAuthenticator = mockk(relaxed = true)

    @Bean
    fun keyManager(): MockKeyManager = MockKeyManager()
}
