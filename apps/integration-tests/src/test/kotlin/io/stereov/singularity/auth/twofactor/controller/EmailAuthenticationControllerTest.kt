package io.stereov.singularity.auth.twofactor.controller

import com.github.michaelbull.result.getOrThrow
import io.mockk.verify
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.test.BaseMailIntegrationTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.*

class EmailAuthenticationControllerTest : BaseMailIntegrationTest() {

    @Test fun `send works with access token`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `send works with 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken!!)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `send works with access and 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken!!)
            .exchange()
            .expectStatus().isOk

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `send requires access or 2fa token`() = runTest {
        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `send requires password authentication`() = runTest {
        val guest = registerOAuth2()

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(guest.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `send is too many attempts when cooldown is active`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `enable works`() = runTest {
        val user = registerUser()

        val setupRes = webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(EnableEmailTwoFactorMethodRequest(user.email2faCode))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        val userWith2fa = userService.findById(user.id).getOrThrow()

        requireNotNull(setupRes)
        assertTrue(userWith2fa.sensitive.security.twoFactor.email.enabled)
        assertTrue(setupRes.twoFactorAuthEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL),setupRes.twoFactorMethods)
        assertEquals(listOf(TwoFactorMethod.EMAIL),userWith2fa.twoFactorMethods)
    }
    @Test fun `enable needs correct code`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(EnableEmailTwoFactorMethodRequest(user.email2faCode + 1))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.email.enabled)
        assertFalse(userWith2fa.twoFactorEnabled)
    }
    @Test fun `enable needs unexpired code`() = runTest {
        val user = registerUser()
        
        user.info.sensitive.security.twoFactor.email.expiresAt = Instant.ofEpochSecond(0)
        userService.save(user.info)
        val code = user.email2faCode

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(EnableEmailTwoFactorMethodRequest(code))
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.email.enabled)
        assertFalse(userWith2fa.twoFactorEnabled)
    }
    @Test fun `enable needs authentication`() = runTest {
        val user = registerUser()

        val code = user.email2faCode

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .bodyValue(EnableEmailTwoFactorMethodRequest(code))
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.email.enabled)
        assertFalse(userWith2fa.twoFactorEnabled)
    }
    @Test fun `enable needs step-up`() = runTest {
        val user = registerUser()

        val code = user.email2faCode

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .bodyValue(EnableEmailTwoFactorMethodRequest(code))
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.email.enabled)
        assertFalse(userWith2fa.twoFactorEnabled)
    }
    @Test fun `enable requires password authentication`() = runTest {
        val user = registerOAuth2()

        val code = user.email2faCode

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .bodyValue(EnableEmailTwoFactorMethodRequest(code))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertFalse(userWith2fa.sensitive.security.twoFactor.email.enabled)
        assertFalse(userWith2fa.twoFactorEnabled)
    }
    @Test fun `enable is not modified when enabled already`() = runTest {
        val user = registerUser(email2faEnabled = true)

        val code = user.email2faCode

        webTestClient.post()
            .uri("/api/auth/2fa/email/enable")
            .bodyValue(EnableEmailTwoFactorMethodRequest(code))
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isNotModified

        val userWith2fa = userService.findById(user.id).getOrThrow()

        assertTrue(userWith2fa.sensitive.security.twoFactor.email.enabled)
        assertTrue(userWith2fa.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL),userWith2fa.twoFactorMethods)
    }

    @Test fun `disable works`() = runTest {
        val user = registerUser(email2faEnabled = true, totpEnabled = true)

        val res = webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .stepUpTokenCookie(user.stepUpToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()

        val body = res.responseBody
        requireNotNull(body)

        assertEquals(user.id, body.id)
        assertTrue(body.twoFactorAuthEnabled)
        assertEquals(listOf(TwoFactorMethod.TOTP), body.twoFactorMethods)

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertEquals(listOf(TwoFactorMethod.TOTP), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.totp.enabled)
        assertFalse(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires another 2fa method to be enabled`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .stepUpTokenCookie(user.stepUpToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires authentication`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires step up token`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires valid step up token`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .stepUpTokenCookie("test")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires unexpired step up token`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpTokenService.create(user.id, user.sessionId, Instant.ofEpochSecond(0)).getOrThrow()).getOrThrow().value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires step up token for same user`() = runTest {
        val user = registerUser(email2faEnabled = true)
        val anotherUser = registerUser()

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .stepUpTokenCookie(cookieCreator.createCookie(stepUpTokenService.create(anotherUser.id, user.sessionId).getOrThrow()).getOrThrow().value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires step up token for same session`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .cookie(
                SessionTokenType.StepUp.cookieName, cookieCreator.createCookie(stepUpTokenService.create(user.id,
                UUID.randomUUID()).getOrThrow()).getOrThrow().value)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        val userAfterDisable = userService.findById(user.id).getOrThrow()
        assertTrue(userAfterDisable.twoFactorEnabled)
        assertEquals(listOf(TwoFactorMethod.EMAIL), userAfterDisable.twoFactorMethods)
        assertTrue(userAfterDisable.sensitive.security.twoFactor.email.enabled)
    }
    @Test fun `disable requires 2fa to be enabled`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/auth/2fa/email")
            .stepUpTokenCookie(user.stepUpToken)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `cooldown works with access token`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/2fa/email/cooldown")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.remaining > 0)
    }
    @Test fun `cooldown works with 2fa token`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken!!)
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/2fa/email/cooldown")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.remaining > 0)
    }
    @Test fun `cooldown works with both tokens`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken!!)
            .exchange()
            .expectStatus().isOk

        val res = webTestClient.get()
            .uri("/api/auth/2fa/email/cooldown")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertTrue(res.remaining > 0)
    }
    @Test fun `cooldown needs token`() = runTest {
        val user = registerUser(email2faEnabled = true)

        webTestClient.post()
            .uri("/api/auth/2fa/email/send")
            .accessTokenCookie(user.accessToken)
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken!!)
            .exchange()
            .expectStatus().isOk

        webTestClient.get()
            .uri("/api/auth/2fa/email/cooldown")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `cooldown works with no cooldown`() = runTest {
        val user = registerUser(email2faEnabled = true)

        val res = webTestClient.get()
            .uri("/api/auth/2fa/email/cooldown")
            .twoFactorAuthenticationTokenCookie(user.twoFactorToken!!)
            .exchange()
            .expectStatus().isOk
            .expectBody(MailCooldownResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remaining)
    }

}
