package io.stereov.singularity.core.global.service.hash

import io.stereov.singularity.core.global.service.hash.HashService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class HashServiceTest {
    private val hashService = HashService()

    @Test
    fun `checkBCrypt works as expected`() {
        val secret = "this is a secret"
        val hashedSecret = hashService.hashBcrypt(secret)

        assertTrue(hashService.checkBcrypt(secret, hashedSecret))
    }
}
