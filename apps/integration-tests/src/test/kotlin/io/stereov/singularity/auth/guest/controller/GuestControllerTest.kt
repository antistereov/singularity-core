package io.stereov.singularity.auth.guest.controller

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.guest.dto.request.ConvertToUserRequest
import io.stereov.singularity.auth.guest.dto.request.CreateGuestRequest
import io.stereov.singularity.auth.guest.dto.response.ConvertToUserResponse
import io.stereov.singularity.auth.guest.dto.response.CreateGuestResponse
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GuestControllerTest : BaseIntegrationTest() {

    @Test fun `createGuest works`() = runTest {
        val name = "Guest"
        val result = webTestClient.post()
            .uri("/api/guests")
            .bodyValue(CreateGuestRequest(name, null))
            .exchange()
            .expectStatus().isOk
            .expectBody(CreateGuestResponse::class.java)
            .returnResult()

        val accessToken = result.extractAccessToken()
        val refreshToken = result.extractRefreshToken()

        val guest = userService.findById(accessToken.userId)

        Assertions.assertEquals(name, guest.sensitive.name)
        Assertions.assertTrue(guest.roles.size == 1)
        Assertions.assertTrue(guest.roles.contains(Role.GUEST))
        Assertions.assertNull(guest.sensitive.email)
        Assertions.assertTrue(guest.sensitive.identities.isEmpty())
        Assertions.assertFalse(guest.twoFactorEnabled)
        Assertions.assertFalse(guest.sensitive.security.email.verified)
        Assertions.assertEquals(1, guest.sensitive.sessions.size)

        Assertions.assertEquals(accessToken.userId, refreshToken.userId)
        Assertions.assertEquals(accessToken.sessionId, guest.sensitive.sessions.keys.first())
        Assertions.assertEquals(accessToken.sessionId, refreshToken.sessionId)
    }
    @Test fun `createGuest saves session info correctly`() = runTest {
        val name = "Guest"
        val req = CreateGuestRequest(name, SessionInfoRequest("browser", "os"))
        val result = webTestClient.post()
            .uri("/api/guests")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(CreateGuestResponse::class.java)
            .returnResult()

        val accessToken = result.extractAccessToken()

        val guest = userService.findById(accessToken.userId)

        Assertions.assertEquals(1, guest.sensitive.sessions.size)
        val session = guest.sensitive.sessions[accessToken.sessionId]!!

        Assertions.assertEquals(req.session!!.browser, session.browser)
        Assertions.assertEquals(req.session!!.os, session.os)
    }
    @Test fun `createGuest is not modified when already authenticated`() = runTest {
        val name = "Guest"
        val user = registerUser()
        webTestClient.post()
            .uri("/api/guests")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CreateGuestRequest(name, null))
            .exchange()
            .expectStatus().isNotModified

        val allUsers = userService.findAll().toList()
        Assertions.assertEquals(1, allUsers.size)
    }

    @Test fun `convertGuestToUser works`() = runTest {
        val guest = createGuest()
        val email = "email@example.com"
        val password = "Password$2"
        val req = ConvertToUserRequest(email, password, null)

        val result = webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(ConvertToUserResponse::class.java)
            .returnResult()

        val accessToken = result.extractAccessToken()
        val refreshToken = result.extractRefreshToken()

        val user = userService.findById(guest.info.id)

        Assertions.assertEquals(user.id, accessToken.userId)
        Assertions.assertEquals(user.id, refreshToken.userId)
        Assertions.assertEquals(1, user.sensitive.sessions.size)

        val session = user.sensitive.sessions.keys.first()

        Assertions.assertEquals(session, accessToken.sessionId)
        Assertions.assertEquals(session, refreshToken.sessionId)

        Assertions.assertEquals(1, user.roles.size)
        Assertions.assertTrue(user.roles.contains(Role.USER))
        Assertions.assertFalse(user.isGuest)

        Assertions.assertEquals(email, user.sensitive.email)
        Assertions.assertTrue(hashService.checkBcrypt(password, user.password!!))

        Assertions.assertEquals(1, user.sensitive.identities.size)
        Assertions.assertEquals(IdentityProvider.PASSWORD, user.sensitive.identities.keys.first())
        Assertions.assertEquals(guest.info.sensitive.name, user.sensitive.name)
    }
    @Test fun `convertGuestToUser saves session info correctly`() = runTest {
        val guest = createGuest()
        val email = "email@example.com"
        val password = "Password$2"
        val req = ConvertToUserRequest(email, password, SessionInfoRequest("browser", "os"))

        val result = webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(ConvertToUserResponse::class.java)
            .returnResult()

        val accessToken = result.extractAccessToken()
        val user = userService.findById(guest.info.id)

        Assertions.assertEquals(1, user.sensitive.sessions.size)
        val session = user.sensitive.sessions[accessToken.sessionId]!!

        Assertions.assertEquals(req.session!!.browser, session.browser)
        Assertions.assertEquals(req.session!!.os, session.os)
    }
    @Test fun `convertGuestToUser requires valid email`() = runTest {
        val guest = createGuest()
        val email = "invalid"
        val password = "Password$2"
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `convertGuestToUser requires non-taken email address`() = runTest {
        val user = registerUser()

        val guest = createGuest()
        val email = user.email!!
        val password = "Password$2"
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }
    @Test fun `convertGuestToUser requires password of min 8 characters`() = runTest {
        val guest = createGuest()
        val email = "email@example.com"
        val password = "Pas$2"
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `convertGuestToUser requires passwort with lower case letter`() = runTest {
        val guest = createGuest()
        val email = "email@example.com"
        val password = "PASSWORD$2"
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `convertGuestToUser requires password with upper-case letter`() = runTest {
        val guest = createGuest()
        val email = "email@example.com"
        val password = "password$2"
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `convertGuestToUser requires password with number`() = runTest {
        val guest = createGuest()
        val email = "email@example.com"
        val password = "Password$"
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `convertGuestToUser requires password with special character`() = runTest {
        val guest = createGuest()
        val email = "email@example.com"
        val password = "Password2"
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `convertGuestToUser needs body`() = runTest {
        val guest = createGuest()

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `convertGuestToUser returns not modified if already authenticated`() = runTest {
        val user = registerUser()
        val email = user.email!!
        val password = user.password!!
        val req = ConvertToUserRequest(email, password, null)

        webTestClient.post()
            .uri("/api/guests/convert-to-user")
            .bodyValue(req)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isNotModified
    }
}