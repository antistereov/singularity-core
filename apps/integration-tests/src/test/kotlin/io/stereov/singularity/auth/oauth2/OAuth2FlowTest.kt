package io.stereov.singularity.auth.oauth2

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OAuth2FlowTest : BaseIntegrationTest() {

    @Test fun `register flow works`() = runTest {
        val sessionInfo = SessionInfoRequest("browser", "os")
        val sessionToken = sessionTokenService.create(sessionInfo)

        val res = webTestClient.get()
            .uri("/oauth2/authorization/github")
            .exchange()
            .expectStatus().isFound
            .expectBody()
            .returnResult()

        println(res.responseHeaders.location)

    }
}