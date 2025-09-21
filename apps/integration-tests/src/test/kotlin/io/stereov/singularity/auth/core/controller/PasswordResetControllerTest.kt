package io.stereov.singularity.auth.core.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant

class PasswordResetControllerTest : BaseMailIntegrationTest() {

    @Test fun `resetPassword works`() = runTest {
        val user = registerUser()
        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret!!)

        Assertions.assertFalse(user.info.sensitive.security.email.verified)

        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findById(user.info.id)

        Assertions.assertNotEquals(
            user.info.sensitive.security.password.resetSecret,
            verifiedUser.sensitive.security.password.resetSecret
        )
        Assertions.assertNotEquals(user.info.password, verifiedUser.password)

        val credentials = LoginRequest(user.info.sensitive.email!!, newPassword, SessionInfoRequest("test"))

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(credentials)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `resetPassword requires token`() = runTest {
        val req = ResetPasswordRequest("test")

        webTestClient.post()
            .uri("/api/auth/password/reset")
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `resetPassword requires valid token`() = runTest {
        val req = ResetPasswordRequest("test")

        webTestClient.post()
            .uri("/api/auth/email/verify?token=test")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `resetPassword requires unexpired token`() = runTest {
        val user = registerUser()
        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret!!, Instant.ofEpochSecond(0))

        val req = ResetPasswordRequest("Test")

        webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `resetPassword needs body`() = runTest {
        val user = registerUser()
        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret!!)


        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `resetPassword is bad for only oauth2`() = runTest {
        val user = registerOAuth2()
        val token = passwordResetTokenService.create(user.info.id, user.info.sensitive.security.password.resetSecret)

        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `resetPassword is bad for guest`() = runTest {
        val guest = createGuest()
        val token = passwordResetTokenService.create(guest.info.id, guest.info.sensitive.security.password.resetSecret)

        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `sendPasswordReset works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email!!))
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `sendPasswordReset requires email`() = runTest {
        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `sendPasswordReset is ok for non-existing email`() = runTest {
        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest("another@email.com"))
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `sendPasswordReset is too many requests when cooldown is active`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email!!))
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email!!))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `passwordRestCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email!!))
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/password/reset/cooldown?email=${user.email}")
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertTrue(res.remaining > 0)
    }
    @Test fun `passwordRestCooldown works when no cooldown`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/password/reset/cooldown?email=${user.email}")
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(0, res.remaining)
    }
    @Test fun `passwordRestCooldown works when no email`() = runTest {
        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest("another@email.com"))
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/password/reset/cooldown?email=another@email.com")
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        Assertions.assertTrue(res!!.remaining > 0)
    }
    @Test fun `passwordRestCooldown needs email`() = runTest {
        webTestClient.get()
            .uri("/api/auth/password/reset/cooldown")
            .exchange()
            .expectStatus().isBadRequest
    }
}