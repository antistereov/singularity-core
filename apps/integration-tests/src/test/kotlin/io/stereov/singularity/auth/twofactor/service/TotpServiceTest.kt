package io.stereov.singularity.auth.twofactor.service

import com.warrenstrange.googleauth.GoogleAuthenticator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TotpServiceTest {

    private val gAuth = GoogleAuthenticator()
    private val totpService = TotpService(gAuth)

    @Test
    fun `2FA works as expected`() {
        val secret = totpService.generateSecretKey()
        val code = totpService.getTotpPassword(secret)

        val isValid = totpService.codeIsValid(secret, code)
        Assertions.assertTrue(isValid, "OTP should be valid")
    }

    @Test
    fun `Invalid code will be invalid`() {
        val secret = totpService.generateSecretKey()
        val code = totpService.getTotpPassword(secret)

        val isValid = totpService.codeIsValid(secret, code + 1)
        Assertions.assertFalse(isValid, "OTP should be invalid")
    }
}