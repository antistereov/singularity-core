package io.stereov.singularity.global.service.twofactorauth

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.global.service.twofactorauth.TwoFactorAuthService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TwoFactorAuthServiceTest {

    private val gAuth = GoogleAuthenticator()
    private val twoFactorAuthService = TwoFactorAuthService(gAuth)

    @Test
    fun `2FA works as expected`() {
        val secret = twoFactorAuthService.generateSecretKey()
        val code = twoFactorAuthService.getTotpPassword(secret)

        val isValid = twoFactorAuthService.validateCode(secret, code)
        assertTrue(isValid, "OTP should be valid")
    }

    @Test
    fun `Invalid code will be invalid`() {
        val secret = twoFactorAuthService.generateSecretKey()
        var code = twoFactorAuthService.getTotpPassword(secret)

        val isValid = twoFactorAuthService.validateCode(secret, ++code)
        assertFalse(isValid, "OTP should be invalid")
    }
}
