package io.stereov.singularity.auth.core.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant

class EmailVerificationControllerTest : BaseMailIntegrationTest() {

    @Test fun `verifyEmail works`() = runTest {
        val user = registerUser()
        val token = emailVerificationTokenService.create(user.info.id, user.info.sensitive.email!! ,user.mailVerificationSecret!!)

        assertFalse(user.info.sensitive.security.email.verified)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isOk

        val verifiedUser = userService.findByEmail(user.info.sensitive.email!!)

        assertTrue(verifiedUser.sensitive.security.email.verified)
    }
    @Test fun `verifyEmail requires token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/email/verification")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `verifyEmail requires valid token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/email/verification?token=test")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail requires right token`() = runTest {
        val user = registerUser()
        val token = emailVerificationTokenService.create(user.info.id, user.info.sensitive.email!!, encryptionSecretService.getCurrentSecret().value)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail requires unexpired token`() = runTest {
        val user = registerUser()
        val token = emailVerificationTokenService.create(user.info.id, user.info.sensitive.email!!, user.mailVerificationSecret!!, Instant.ofEpochSecond(0))

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `verifyEmail returns not modified when already verified`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.email.verified = true
        userService.save(user.info)

        val token = emailVerificationTokenService.create(user.info.id, user.info.sensitive.email!! ,user.mailVerificationSecret!!)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isNotModified
    }
    @Test fun `verifyEmail is bad for guest`() = runTest {
        val guest = createGuest()

        val token = emailVerificationTokenService.create(guest.info.id, "random-email" ,guest.info.sensitive.security.email.verificationSecret)

        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `sendVerificationEmail works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `sendVerificationEmail requires authentication`() = runTest {
        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `sendVerificationEmail returns not modified when already verified`() = runTest {
        val user = registerUser()
        user.info.sensitive.security.email.verified = true
        userService.save(user.info)

        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isNotModified
    }
    @Test fun `sendVerificationEmail is bad for guest`() = runTest {
        val guest = createGuest()

        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `sendVerificationEmail is too many attempts when cooldown is active`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `verifyCooldown works`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/email/verification/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/email/verification/cooldown")
            .accessTokenCookie(user.accessToken)
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
            .uri("/api/auth/email/verification/cooldown")
            .accessTokenCookie(user.accessToken)
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
            .uri("/api/auth/email/verification/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
