package io.stereov.singularity.auth.core.controller

import io.mockk.verify
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class EmailVerificationController : BaseMailIntegrationTest() {

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
}