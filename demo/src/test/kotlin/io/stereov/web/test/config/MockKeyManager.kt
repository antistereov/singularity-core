package io.stereov.web.test.config

import io.mockk.every
import io.mockk.mockk
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.global.service.secrets.model.Secret
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.util.*
import javax.crypto.KeyGenerator

@TestConfiguration
class MockKeyManager {

    @Bean
    fun keyManager(): KeyManager = mockk<KeyManager>(relaxed = true).apply {
        val uuid = UUID.randomUUID()

        every { generateKey(any(), any()) } answers  {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)

            Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
        }

        val key = this.generateKey()

        val secret = Secret(uuid, key)
        every { getSecretById(any()) } returns secret
        every { getEncryptionSecret() } returns secret
        every { updateEncryptionSecret() } returns secret
        every { getJwtSecret() } returns secret
        every { updateJwtSecret() } returns secret


    }
}
