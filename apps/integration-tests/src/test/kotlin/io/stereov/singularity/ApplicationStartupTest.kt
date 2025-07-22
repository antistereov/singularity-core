package io.stereov.singularity

import io.stereov.singularity.mail.service.MailServiceImpl
import io.stereov.singularity.user.service.mail.MailTokenService
import io.stereov.singularity.mail.properties.MailProperties
import io.stereov.singularity.user.controller.UserMailController
import io.stereov.singularity.user.service.mail.UserMailService
import io.stereov.singularity.test.BaseIntegrationTest
import org.junit.jupiter.api.Test

class ApplicationStartupTest : BaseIntegrationTest() {

    @Test
    fun `application context loads`() {}

    @Test fun `non-existing path returns 404`() {
        webTestClient.get()
            .uri("/this/path/does/not/exist")
            .exchange()
            .expectStatus().isNotFound
    }


    @Test
    fun `context loads`() {
        applicationContext.getBean(MailProperties::class.java)
        applicationContext.getBean(MailServiceImpl::class.java)
        applicationContext.getBean(MailTokenService::class.java)
        applicationContext.getBean(UserMailService::class.java)
        applicationContext.getBean(UserMailController::class.java)
    }

}
