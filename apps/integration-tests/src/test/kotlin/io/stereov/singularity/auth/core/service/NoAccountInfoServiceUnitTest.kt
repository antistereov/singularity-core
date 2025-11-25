package io.stereov.singularity.auth.core.service

import io.mockk.verify
import io.stereov.singularity.auth.alert.service.NoAccountInfoService
import io.stereov.singularity.auth.core.model.NoAccountInfoAction
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NoAccountInfoServiceUnitTest : BaseMailIntegrationTest() {

    @Autowired
    private lateinit var noAccountInfoService: NoAccountInfoService

    @Test fun `cooldown works with password reset`() = runTest {
        noAccountInfoService.send("email@test.com", NoAccountInfoAction.PASSWORD_RESET,null)
        noAccountInfoService.send("email@test.com", NoAccountInfoAction.PASSWORD_RESET,null)

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }
}