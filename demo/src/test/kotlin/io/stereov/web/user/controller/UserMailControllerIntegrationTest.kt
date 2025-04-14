package io.stereov.web.user.controller

import io.mockk.verify
import io.stereov.web.config.Constants
import io.stereov.web.global.service.mail.MailTokenService
import io.stereov.web.test.BaseMailIntegrationTest
import io.stereov.web.user.dto.request.DeviceInfoRequest
import io.stereov.web.user.dto.request.LoginRequest
import io.stereov.web.user.dto.response.MailCooldownResponse
import io.stereov.web.user.dto.request.ResetPasswordRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import java.time.Instant

class UserMailControllerIntegrationTest : BaseMailIntegrationTest() {

    @Autowired
    private lateinit var mailTokenService: MailTokenService

    @Test fun `verifyEmail works`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createVerificationToken(user.info.email, user.info.security.mail.verificationSecret)

        assertFalse(user.info.security.mail.verified)

        webTestClient.post()
            .uri("/user/mail/verify?token=$token")
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findByEmail(user.info.email)

        assertTrue(verifiedUser.security.mail.verified)
    }
    @Test fun `verifyEmail requires token`() = runTest {
        webTestClient.post()
            .uri("/user/mail/verify")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyEmail requires valid token`() = runTest {
        webTestClient.post()
            .uri("/user/mail/verify?token=test")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail requires unexpired token`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createVerificationToken(user.info.email, user.info.security.mail.verificationSecret, Instant.ofEpochSecond(0))

        webTestClient.post()
            .uri("/user/mail/verify?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `sendVerificationEmail works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/mail/verify/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<SimpleMailMessage>()) }
    }

    @Test fun `verifyCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/mail/verify/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/user/mail/verify/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
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
            .uri("/user/mail/verify/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
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
            .uri("/user/mail/verify/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `resetPassword works`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createPasswordResetToken(user.info.id, user.info.security.mail.passwordResetSecret)

        assertFalse(user.info.security.mail.verified)

        val newPassword = "new-password878"
        val req = ResetPasswordRequest(newPassword)

        webTestClient.post()
            .uri("/user/mail/reset-password?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findById(user.info.id)

        assertNotEquals(user.info.security.mail.passwordResetSecret, verifiedUser.security.mail.passwordResetSecret)
        assertNotEquals(user.info.password, verifiedUser.password)

        val credentials = LoginRequest(user.info.email, newPassword, DeviceInfoRequest("test"))

        webTestClient.post()
            .uri("/user/login")
            .bodyValue(credentials)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `resetPassword requires token`() = runTest {
        val req = ResetPasswordRequest("test")

        webTestClient.post()
            .uri("/user/mail/reset-password")
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `resetPassword requires valid token`() = runTest {
        val req = ResetPasswordRequest("test")

        webTestClient.post()
            .uri("/user/mail/verify?token=test")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `resetPassword requires unexpired token`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createPasswordResetToken(user.info.email, user.info.security.mail.passwordResetSecret, Instant.ofEpochSecond(0))

        val req = ResetPasswordRequest("Test")

        webTestClient.post()
            .uri("/user/mail/verify?token=$token")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `resetPassword needs body`() = runTest {
        val user = registerUser()
        val token = mailTokenService.createPasswordResetToken(user.info.email, user.info.security.mail.passwordResetSecret)


        webTestClient.post()
            .uri("/user/mail/reset-password?token=$token")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `sendPasswordReset works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/mail/reset-password/send?email=${user.info.email}")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<SimpleMailMessage>()) }
    }
    @Test fun `sendPasswordReset requires email`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/mail/reset-password/send")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `passwordRestCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/user/mail/reset-password/send?email=${user.info.email}")
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/user/mail/reset-password/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
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
            .uri("/user/mail/reset-password/cooldown")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
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
            .uri("/user/mail/reset-password/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
