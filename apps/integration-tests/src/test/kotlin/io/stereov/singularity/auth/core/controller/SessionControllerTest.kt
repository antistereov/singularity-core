package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import java.util.*

class SessionControllerTest : BaseIntegrationTest() {

    @Test fun `getSessions returns sessions`() = runTest {
        val user = registerUser()
        val sessionId2 = UUID.randomUUID()
        val sessionId3 = UUID.randomUUID()
        user.info.sensitive.sessions[sessionId2] = SessionInfo()
        user.info.sensitive.sessions[sessionId3] = SessionInfo()

        userService.save(user.info)

        val response = webTestClient.get()
            .uri("/api/auth/sessions")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(object : ParameterizedTypeReference<List<SessionInfoResponse>>() {})
            .returnResult()
            .responseBody

        requireNotNull(response) { "No body found in response" }

        Assertions.assertEquals(3, response.size)
        Assertions.assertTrue(response.any { it.id == user.sessionId })
        Assertions.assertTrue(response.any { it.id == sessionId2 })
        Assertions.assertTrue(response.any { it.id == sessionId3 })
    }
    @Test fun `getSessions requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/auth/sessions")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `removeSession deletes session`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/sessions/${user.sessionId}")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        val updatedUser = userService.findById(user.info.id)
        val sessions = updatedUser.sensitive.sessions

        Assertions.assertEquals(0, sessions.size)
    }
    @Test fun `removeSession requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/auth/sessions/${UUID.randomUUID()}")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `clearSessions deletes sessions`() = runTest {
        val user = registerUser()
        webTestClient.delete()
            .uri("/api/auth/sessions")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus()
            .isOk

        val updatedUser = userService.findById(user.info.id)
        val sessions = updatedUser.sensitive.sessions

        Assertions.assertEquals(0, sessions.size)
    }
    @Test fun `clearSessions requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/auth/sessions")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
