package io.stereov.singularity.auth.twofactor.service

import com.github.michaelbull.result.getOrThrow
import com.warrenstrange.googleauth.GoogleAuthenticator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TotpServiceTest {

    private val gAuth = GoogleAuthenticator()
    private val totpService = TotpService(gAuth)

    @Test
    fun `2FA works as expected`() {
        val secret = totpService.generateSecretKey().getOrThrow()
        val code = totpService.getTotpPassword(secret).getOrThrow()

        val isValid = totpService.codeIsValid(secret, code).getOrThrow()
        Assertions.assertTrue(isValid, "OTP should be valid")
    }

    @Test
    fun `Invalid code will be invalid`() {
        val secret = totpService.generateSecretKey().getOrThrow()
        val code = totpService.getTotpPassword(secret).getOrThrow()

        val isValid = totpService.codeIsValid(secret, code + 1).getOrThrow()
        Assertions.assertFalse(isValid, "OTP should be invalid")
    }
}