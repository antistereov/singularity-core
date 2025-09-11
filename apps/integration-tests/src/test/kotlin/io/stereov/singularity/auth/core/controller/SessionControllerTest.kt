package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.model.SessionInfo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import java.time.Instant

class SessionControllerTest : BaseIntegrationTest() {

    @Test fun `getSessions returns sessions`() = runTest {
        val user = registerUser(sessionId = "first")
        user.info.sensitive.sessions.addAll(
            listOf(
                SessionInfo("second", issuedAt = Instant.now()),
                SessionInfo("third", issuedAt = Instant.now())
            )
        )

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
        Assertions.assertTrue(response.any { it.id == "first" })
        Assertions.assertTrue(response.any { it.id == "second" })
        Assertions.assertTrue(response.any { it.id == "third" })
    }
    @Test fun `getSessions requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/auth/sessions")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `removeSession deletes session`() = runTest {
        val sessionId = "session"
        val user = registerUser(sessionId = sessionId)

        webTestClient.delete()
            .uri("/api/auth/sessions/$sessionId")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        val updatedUser = userService.findById(user.info.id)
        val sessions = updatedUser.sensitive.sessions

        Assertions.assertEquals(0, sessions.size)
    }
    @Test fun `removeSession requires authentication`() = runTest {
        webTestClient.delete()
            .uri("/api/auth/sessions/session")
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