package io.stereov.singularity

import io.stereov.singularity.mail.core.properties.MailProperties
import io.stereov.singularity.mail.core.service.MailService
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.mail.user.controller.UserMailController
import io.stereov.singularity.mail.user.service.MailTokenService
import io.stereov.singularity.mail.user.service.UserMailService
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
        applicationContext.getBean(MailService::class.java)
        applicationContext.getBean(MailTokenService::class.java)
        applicationContext.getBean(UserMailService::class.java)
        applicationContext.getBean(UserMailController::class.java)
    }

}
