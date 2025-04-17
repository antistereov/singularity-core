package io.stereov.web.global.service.secrets

import io.stereov.web.global.service.encryption.service.EncryptionService
import io.stereov.web.test.config.MockKeyManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class EncryptionServiceTest {
    private val mock = MockKeyManager().keyManager()

    private val encryptionService = EncryptionService(mock)

    @BeforeEach
    fun mock() {

    }

    @Test fun `encryption works`() {
        val originalText = "Hello, World!"
        val encryptedText = encryptionService.encrypt(originalText)
        val decryptedText = encryptionService.decrypt(encryptedText)

        assertEquals(decryptedText, originalText)
    }
}
