package io.stereov.singularity.test.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.mockk.mockk
import io.stereov.singularity.file.core.service.FileStorage
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockConfig {
    @Bean
    fun googleAuthenticator(): GoogleAuthenticator = mockk(relaxed = true)

    @Bean
    fun keyManager(): MockSecretStore = MockSecretStore()

    @Bean
    fun fileStorage(): FileStorage = mockk(relaxed = true)
}
