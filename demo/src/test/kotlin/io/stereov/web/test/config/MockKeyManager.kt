package io.stereov.web.test.config

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
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

    val encryptionId = UUID.fromString("05c3edb6-3210-4fae-ba41-f3241bbb432f")
    val newEncryptionId = UUID.fromString("e744ae85-3209-4bab-8af9-20e17cd17c3f")

    val jwtId = UUID.fromString("554b7a4b-4eb8-4a6a-b54f-5524a8aa2ce0")
    val newJwtId = UUID.fromString("e7a29b87-896e-4c4e-ad17-aea825f1db66")


    @Bean
    fun keyManager(): KeyManager = mockk<KeyManager>(relaxed = true).apply {

        val logger: KLogger = KotlinLogging.logger {}

        every { generateKey(any(), "AES") } answers  {
            logger.debug { "Generating encryption key" }
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)

            Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
        }
        every { generateKey(any(), "HmacSHA256") } answers  {
            logger.debug { "Generating jwt key" }
            val keyGenerator = KeyGenerator.getInstance("HmacSHA256")
            keyGenerator.init(256)

            Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
        }

        val encryptionSecret = Secret(encryptionId, this.generateKey(algorithm = "AES"))
        val newEncryptionSecret = Secret(newEncryptionId, this.generateKey(algorithm = "AES"))
        var currentEncryptionSecret = encryptionSecret

        val jwtSecret = Secret(jwtId, this.generateKey(algorithm = "HmacSHA256"))
        val newJwtSecret = Secret(newJwtId, this.generateKey(algorithm = "HmacSHA256"))
        var currentJwtSecret = jwtSecret

        every { getSecretById(encryptionId) } answers {
            logger.debug { "Getting secret by ID: ${jwtSecret.id}" }
            encryptionSecret
        }
        every { getSecretById(newEncryptionId) } answers {
            logger.debug { "Getting secret by ID: ${newJwtSecret.id}" }
            newEncryptionSecret
        }
        every { getSecretById(jwtId) } answers {
            logger.debug { "Getting secret by ID: ${jwtSecret.id}" }
            jwtSecret
        }
        every { getSecretById(newJwtId) } answers {
            logger.debug { "Getting secret by ID: ${newJwtSecret.id}" }
            newJwtSecret
        }
        every { getEncryptionSecret() } answers {
            logger.debug { "Getting current encryption secret: ${currentEncryptionSecret.id}" }
            currentEncryptionSecret
        }
        every { updateEncryptionSecret() } answers {
            currentEncryptionSecret = newEncryptionSecret
            logger.debug { "Updating encryption secret: ${currentEncryptionSecret.id}" }
            currentEncryptionSecret
        }
        every { getJwtSecret() } answers  {
            logger.debug { "Getting current jwt secret: ${currentJwtSecret.id}" }
            currentJwtSecret
        }
        every { updateJwtSecret() } answers {
            currentJwtSecret = newJwtSecret
            logger.debug { "Updating jwt secret: ${currentJwtSecret.id}" }
            currentJwtSecret
        }
    }
}
