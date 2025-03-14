package io.stereov.web.global.service.twofactorauth

import com.warrenstrange.googleauth.GoogleAuthenticator
import org.springframework.stereotype.Service

@Service
class TwoFactorAuthService {

    private val gAuth = GoogleAuthenticator()

    fun generateSecretKey(): String {
        return gAuth.createCredentials().key
    }

    fun validateCode(secret: String, code: Int): Boolean {
        return gAuth.authorize(secret, code)
    }

    fun getTotpPassword(secret: String): Int {
        return gAuth.getTotpPassword(secret)
    }

    fun getOtpAuthUrl(username: String, secret: String): String {
        return "otpauth://totp/$username?secret=$secret&issuer="
    }
}
