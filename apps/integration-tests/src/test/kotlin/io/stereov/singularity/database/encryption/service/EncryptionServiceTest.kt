package io.stereov.singularity.database.encryption.service

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EncryptionServiceTest : BaseIntegrationTest() {

    @Test
    fun rotationWorks() = runTest {
        val user = registerUser()

        val encryptedUser = userService.findEncryptedById(user.id).getOrThrow()
        val oldSecret = encryptedUser.sensitive.secretKey

        val decryptedUser = userService.decrypt(encryptedUser).getOrThrow()

        encryptionSecretService.updateSecret()
        userService.rotateSecret()

        val rotatedEncryptedUser = userService.findEncryptedById(user.id).getOrThrow()
        val newSecret = rotatedEncryptedUser.sensitive.secretKey

        Assertions.assertNotEquals(oldSecret, newSecret)

        val newDecryptedUser = userService.decrypt(rotatedEncryptedUser).getOrThrow()

        Assertions.assertEquals(decryptedUser, newDecryptedUser)
    }
}