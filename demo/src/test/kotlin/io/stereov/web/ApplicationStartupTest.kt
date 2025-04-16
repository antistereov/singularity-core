package io.stereov.web

import io.stereov.web.global.service.mail.MailService
import io.stereov.web.global.service.mail.MailTokenService
import io.stereov.web.properties.MailProperties
import io.stereov.web.test.BaseIntegrationTest
import io.stereov.web.user.controller.UserMailController
import io.stereov.web.user.service.mail.UserMailService
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
