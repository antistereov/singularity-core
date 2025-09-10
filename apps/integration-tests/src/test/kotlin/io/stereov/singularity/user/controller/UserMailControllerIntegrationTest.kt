package io.stereov.singularity.user.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.model.SessionTokenType
import io.stereov.singularity.auth.core.service.EmailVerificationTokenService
import io.stereov.singularity.auth.core.service.PasswordResetTokenService
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.test.BaseMailIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
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
    private lateinit var emailVerificationTokenService: EmailVerificationTokenService

    @Autowired
    private lateinit var passwordResetTokenService: PasswordResetTokenService

    @Test fun `verifyEmail works`() = runTest {
        val user = registerUser()
        val token = emailVerificationTokenService.create(user.info.id, user.info.sensitive.email,user.mailVerificationSecret)

        assertFalse(user.info.sensitive.security.mail.verified)

        webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findByEmail(user.info.sensitive.email)

        assertTrue(verifiedUser.sensitive.security.mail.verified)
    }
    @Test fun `verifyEmail requires token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/email/verify")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyEmail requires valid token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/email/verify?token=test")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail requires right token`() = runTest {
        val user = registerUser()
        val token = emailVerificationTokenService.create(user.info.id, user.info.sensitive.email, encryptionSecretService.getCurrentSecret().value)

        webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail requires unexpired token`() = runTest {
        val user = registerUser()
        val token = emailVerificationTokenService.create(user.info.id, user.info.sensitive.email, user.mailVerificationSecret, Instant.ofEpochSecond(0))

        webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `sendVerificationEmail works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/email/verify/send")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `verifyCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/email/verify/send")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/email/verify/cooldown")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.remaining > 0)
    }
    @Test fun `verifyCooldown works when no cooldown`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/email/verify/cooldown")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remaining)
    }
    @Test fun `verifyCooldown requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/auth/email/verify/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `changeEmail does nothing without validation`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "password"
        val user = registerUser(oldEmail, password)

        val res = webTestClient.put()
            .uri("/api/users/me/email")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
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
        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret)

        assertFalse(user.info.sensitive.security.mail.verified)

        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findById(user.info.id)

        assertNotEquals(user.info.sensitive.security.mail.passwordResetSecret, verifiedUser.sensitive.security.mail.passwordResetSecret)
        assertNotEquals(user.info.password, verifiedUser.password)

        val credentials = LoginRequest(user.info.sensitive.email, newPassword, SessionInfoRequest("test"))

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
        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret, Instant.ofEpochSecond(0))

        val req = ResetPasswordRequest("Test")

        webTestClient.post()
            .uri("/api/auth/email/verify?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `resetPassword needs body`() = runTest {
        val user = registerUser()
        val token = passwordResetTokenService.create(user.info.id, user.passwordResetSecret)


        webTestClient.post()
            .uri("/api/auth/password/reset?token=$token")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `sendPasswordReset works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email))
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `sendPasswordReset requires email`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `passwordRestCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/password/reset-request")
            .bodyValue(SendPasswordResetRequest(user.info.sensitive.email))
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/password/reset/cooldown")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.remaining > 0)
    }
    @Test fun `passwordRestCooldown works when no cooldown`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/auth/password/reset/cooldown")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remaining)
    }
    @Test fun `passwordRestCooldown requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/auth/password/reset/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
