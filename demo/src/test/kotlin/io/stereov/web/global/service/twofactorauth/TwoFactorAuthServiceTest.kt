package io.stereov.web.global.service.twofactorauth

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.web.global.service.encryption.EncryptionService
import io.stereov.web.properties.EncryptionProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TwoFactorAuthServiceTest {

    private val encryptionProperties = EncryptionProperties("3eJAiq7XBjMc5AXkCwsjbA==")
    private val gAuth = GoogleAuthenticator()
    private val encryptionService = EncryptionService(encryptionProperties)
    private val twoFactorAuthService = TwoFactorAuthService(gAuth, encryptionService)

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
