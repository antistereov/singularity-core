package io.stereov.singularity.user.controller

import io.mockk.verify
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.test.BaseMailIntegrationTest
import io.stereov.singularity.user.dto.UserResponse
import io.stereov.singularity.user.dto.request.*
import io.stereov.singularity.user.service.mail.MailTokenService
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class UserMailControllerIntegrationTest : BaseMailIntegrationTest() {

    @Autowired
    private lateinit var encryptionSecretService: EncryptionSecretService

    @Autowired
    private lateinit var mailTokenService: MailTokenService

    @Test fun `verifyEmail works`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createVerificationToken(user.info.id, user.info.sensitive.email,user.mailVerificationSecret)

        assertFalse(user.info.sensitive.security.mail.verified)

        webTestClient.post()
            .uri("/api/user/mail/verify?token=$token")
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findByEmail(user.info.sensitive.email)

        assertTrue(verifiedUser.sensitive.security.mail.verified)
    }
    @Test fun `verifyEmail requires token`() = runTest {
        webTestClient.post()
            .uri("/api/user/mail/verify")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyEmail requires valid token`() = runTest {
        webTestClient.post()
            .uri("/api/user/mail/verify?token=test")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail requires right token`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createVerificationToken(user.info.id, user.info.sensitive.email, encryptionSecretService.getCurrentSecret().value)

        webTestClient.post()
            .uri("/api/user/mail/verify?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail requires unexpired token`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createVerificationToken(user.info.id, user.info.sensitive.email, user.mailVerificationSecret, Instant.ofEpochSecond(0))

        webTestClient.post()
            .uri("/api/user/mail/verify?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `sendVerificationEmail works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/mail/verify/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `verifyCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/mail/verify/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/user/mail/verify/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(io.stereov.singularity.user.dto.response.MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.remaining > 0)
    }
    @Test fun `verifyCooldown works when no cooldown`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/user/mail/verify/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(io.stereov.singularity.user.dto.response.MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remaining)
    }
    @Test fun `verifyCooldown requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/user/mail/verify/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `changeEmail does nothing without validation`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        val res = webTestClient.put()
            .uri("/api/user/me/email")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail, password))
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

    @Test fun `resetPassword works`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createPasswordResetToken(user.info.id, user.passwordResetSecret)

        assertFalse(user.info.sensitive.security.mail.verified)

        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/user/mail/reset-password?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findById(user.info.id)

        assertNotEquals(user.info.sensitive.security.mail.passwordResetSecret, verifiedUser.sensitive.security.mail.passwordResetSecret)
        assertNotEquals(user.info.password, verifiedUser.password)

        val credentials = LoginRequest(user.info.sensitive.email, newPassword, DeviceInfoRequest("test"))

        webTestClient.post()
            .uri("/api/user/login")
            .bodyValue(credentials)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `resetPassword requires token`() = runTest {
        val req = ResetPasswordRequest("test")

        webTestClient.post()
            .uri("/api/user/mail/reset-password")
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `resetPassword requires valid token`() = runTest {
        val req = ResetPasswordRequest("test")

        webTestClient.post()
            .uri("/api/user/mail/verify?token=test")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `resetPassword requires unexpired token`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createPasswordResetToken(user.info.id, user.passwordResetSecret, Instant.ofEpochSecond(0))

        val req = ResetPasswordRequest("Test")

        webTestClient.post()
            .uri("/api/user/mail/verify?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `resetPassword needs body`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createPasswordResetToken(user.info.id, user.passwordResetSecret)


        webTestClient.post()
            .uri("/api/user/mail/reset-password?token=$token")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `sendPasswordReset works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/mail/reset-password/send")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email))
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `sendPasswordReset requires email`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/mail/reset-password/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `passwordRestCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/user/mail/reset-password/send")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email))
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/user/mail/reset-password/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(io.stereov.singularity.user.dto.response.MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.remaining > 0)
    }
    @Test fun `passwordRestCooldown works when no cooldown`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/user/mail/reset-password/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(io.stereov.singularity.user.dto.response.MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remaining)
    }
    @Test fun `passwordRestCooldown requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/user/mail/reset-password/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
