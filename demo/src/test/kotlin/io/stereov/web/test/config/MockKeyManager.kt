package io.stereov.web.test.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.stereov.web.global.service.encryption.component.KeyManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockKeyManager {


    @Bean
    fun keyManager(): KeyManager = mockk<KeyManager>(relaxed = true).apply {
        every { getKeyById(any()) } returns "id"
        every { getKeyById(any()) } returns "3eJAiq7XBjMc5AXkCwsjbA=="
        every { initializeKeysFromEnv() } just runs
    }

}
