package io.stereov.singularity.auth.core.service

import io.mockk.verify
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IdentityProviderInfoServiceUnitTest : BaseMailIntegrationTest() {

    @Autowired
    private lateinit var identityProviderInfoService: IdentityProviderInfoService

    @Test fun `cooldown works`() = runTest {
        val user = registerUser()

        identityProviderInfoService.send(user.info, null)
        identityProviderInfoService.send(user.info, null)

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }
}