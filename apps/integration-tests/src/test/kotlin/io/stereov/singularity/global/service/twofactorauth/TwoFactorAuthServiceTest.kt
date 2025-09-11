package io.stereov.singularity.global.service.twofactorauth

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.twofactor.service.TotpService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TwoFactorAuthServiceTest {

    private val gAuth = GoogleAuthenticator()
    private val totpService = TotpService(gAuth)

    @Test
    fun `2FA works as expected`() {
        val secret = totpService.generateSecretKey()
        val code = totpService.getTotpPassword(secret)

        val isValid = totpService.codeIsValid(secret, code)
        assertTrue(isValid, "OTP should be valid")
    }

    @Test
    fun `Invalid code will be invalid`() {
        val secret = totpService.generateSecretKey()
        val code = totpService.getTotpPassword(secret)

        val isValid = totpService.codeIsValid(secret, code + 1)
        assertFalse(isValid, "OTP should be invalid")
    }
}
