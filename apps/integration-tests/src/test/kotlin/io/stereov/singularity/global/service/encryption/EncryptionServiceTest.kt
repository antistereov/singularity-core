package io.stereov.singularity.global.service.encryption

import io.stereov.singularity.encryption.service.EncryptionSecretService
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EncryptionServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var encryptionSecretService: EncryptionSecretService

    @Test fun rotationWorks() = runTest {
        val user = registerUser()

        val encryptedUser = userService.findEncryptedById(user.info.id)
        val oldSecret = encryptedUser.sensitive.secretKey

        val decryptedUser = userService.decrypt(encryptedUser)

        encryptionSecretService.updateSecret()
        userService.rotateSecret()

        val rotatedEncryptedUser = userService.findEncryptedById(user.info.id)
        val newSecret = rotatedEncryptedUser.sensitive.secretKey

        assertNotEquals(oldSecret, newSecret)

        val newDecryptedUser = userService.decrypt(rotatedEncryptedUser)

        assertEquals(decryptedUser, newDecryptedUser)
    }
}
