package io.stereov.web.global.service.encryption

import io.stereov.web.global.service.encryption.service.EncryptionService
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.test.BaseIntegrationTest
import io.stereov.web.test.config.MockKeyManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EncryptionServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var encryptionTransformer: EncryptionService

    @Autowired
    private lateinit var keyManager: KeyManager

    @Autowired
    private lateinit var mockKeyManager: MockKeyManager

    @Test fun rotationWorks() = runTest {
        val user = registerUser()

        val encryptedUser = userService.findEncryptedById(user.info.id)
        val oldSecret = encryptedUser.sensitive.secretId
        assertEquals(mockKeyManager.encryptionId, oldSecret)

        val decryptedUser = userService.decrypt(encryptedUser)

        keyManager.updateEncryptionSecret()
        userService.rotateKey()

        val rotatedEncryptedUser = userService.findEncryptedById(user.info.id)
        val newSecret = rotatedEncryptedUser.sensitive.secretId
        assertEquals(mockKeyManager.newEncryptionId, newSecret)

        assertNotEquals(oldSecret, newSecret)

        val newDecryptedUser = userService.decrypt(rotatedEncryptedUser)

        assertEquals(decryptedUser, newDecryptedUser)
    }
}
