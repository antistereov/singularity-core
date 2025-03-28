package io.stereov.web.global.service.encryption

import io.stereov.web.properties.EncryptionProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EncryptionServiceTest {

    private val encryptionProperties = EncryptionProperties("3eJAiq7XBjMc5AXkCwsjbA==")
    private val encryptionService = EncryptionService(encryptionProperties)

    @Test fun `encryption works`() {
        val originalText = "Hello, World!"
        val encryptedText = encryptionService.encrypt(originalText)
        val decryptedText = encryptionService.decrypt(encryptedText)

        assertEquals(decryptedText, originalText)
    }
}
