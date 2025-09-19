package io.stereov.singularity.user.settings.controller

import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.user.settings.dto.request.ChangeUserRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.client.MultipartBodyBuilder
import java.time.Instant
import java.util.*

class UserSettingsControllerTest : BaseIntegrationTest() {

    @Test fun `changeEmail works with 2fa`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)


        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.info.id, user.sessionId).value
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val token = emailVerificationTokenService.create(user.info.id, newEmail, user.mailVerificationSecret)

        val res = webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newEmail, res.email)
        userService.findByEmail(newEmail)
    }
    @Test fun `changeEmail works without 2fa`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val token = emailVerificationTokenService.create(user.info.id, newEmail, user.mailVerificationSecret)

        val res = webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newEmail, res.email)
        userService.findByEmail(newEmail)
    }
    @Test fun `changeEmail changes email`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)

        val res = webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)
        assertEquals(newEmail, res.email)
        val foundUser = userService.findByEmail(newEmail)
        assertEquals(user.info.id, foundUser.id)
    }
    @Test fun `changeEmail requires authentication`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires body`() = runTest {
        val oldEmail = "old@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `changeEmail requires correct password`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same user`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)
        val anotherUser = registerUser("ttest@email.com")

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(anotherUser.info.id, user.sessionId).value
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same session`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.info.id, UUID.randomUUID()).value
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires unexpired step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(
                    user.info.id,
                    user.sessionId,
                    Instant.ofEpochSecond(0)
                ).value
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires valid step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                "wrong-token"
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires non-existing email`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)
        registerUser(newEmail)

        webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }
    @Test fun `changeEmail does nothing without validation`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)

        val res = webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)
        assertEquals(oldEmail, res.email)
        val foundUser = userService.findByEmail(oldEmail)
        assertEquals(user.info.id, foundUser.id)
    }

    @Test fun `changePassword works with 2fa`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        val res = webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.info.id, user.sessionId).value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email, newPassword, SessionInfoRequest("session")))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `changePassword works without 2fa`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword)

        val res = webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(email, newPassword, SessionInfoRequest("session")))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `changePassword requires authentication`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires body`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val user = registerUser(email, oldPassword)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `changePassword requires correct password`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangePasswordRequest("wrong-password", newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up token for same user`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)
        val anotherUser = registerUser("another@email.com")

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(anotherUser.info.id, user.sessionId).value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires step up token for same session`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.info.id, UUID.randomUUID()).value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires unexpired step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(
                    user.info.id,
                    user.sessionId,
                    Instant.ofEpochSecond(0)
                ).value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changePassword requires valid step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, twoFactorEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .cookie(SessionTokenType.StepUp.cookieName, "wrong-token")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `changeUser works`() = runTest {
        val user = registerUser()
        val newName = "MyName"
        val accessToken = user.accessToken

        val res = webTestClient.put()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .bodyValue(ChangeUserRequest(newName))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newName, res.name)
        assertEquals(newName, userService.findById(user.info.id).sensitive.name)
    }
    @Test fun `changeUser requires authentication`() = runTest {
        registerUser()
        val newName = "MyName"

        webTestClient.put()
            .uri("/api/users/me")
            .bodyValue(ChangeUserRequest(newName))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeUser requires body`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken

        webTestClient.put()
            .uri("/api/users/me")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `setAvatar works`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken

        webTestClient.put()
            .uri("/api/users/me/avatar")
            .cookie(SessionTokenType.Access.cookieName, accessToken)
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part("file", ClassPathResource("files/test-image.jpg"))
                }.build()
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody
    }
}
