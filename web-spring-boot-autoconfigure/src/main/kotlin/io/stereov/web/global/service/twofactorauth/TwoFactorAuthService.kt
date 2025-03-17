package io.stereov.web.global.service.twofactorauth

import com.warrenstrange.googleauth.GoogleAuthenticator
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class TwoFactorAuthService {

    private val gAuth = GoogleAuthenticator()
    private val random = SecureRandom()

    fun generateSecretKey(): String {
        return gAuth.createCredentials().key
    }

    fun validateCode(secret: String, code: Int): Boolean {
        return gAuth.authorize(secret, code)
    }

    fun getTotpPassword(secret: String): Int {
        return gAuth.getTotpPassword(secret)
    }

    fun generateRecoveryCode(length: Int = 10): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    fun getOtpAuthUrl(username: String, secret: String): String {
        return "otpauth://totp/$username?secret=$secret&issuer="
    }
}
