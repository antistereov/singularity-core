package io.stereov.singularity

import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.auth.core.controller.EmailVerificationController
import io.stereov.singularity.auth.token.service.EmailVerificationTokenService
import io.stereov.singularity.auth.core.service.EmailVerificationService
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
        applicationContext.getBean(EmailProperties::class.java)
        applicationContext.getBean(EmailService::class.java)
        applicationContext.getBean(EmailVerificationTokenService::class.java)
        applicationContext.getBean(EmailVerificationService::class.java)
        applicationContext.getBean(EmailVerificationController::class.java)
    }

}
