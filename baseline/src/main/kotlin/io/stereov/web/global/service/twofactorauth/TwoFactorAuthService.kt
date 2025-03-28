package io.stereov.web.global.service.twofactorauth

import com.warrenstrange.googleauth.GoogleAuthenticator
import org.springframework.stereotype.Service
import java.security.SecureRandom

/**
 * # Service for Two-Factor Authentication (2FA).
 *
 * This service provides methods for generating and validating
 * two-factor authentication codes using the Google Authenticator.
 *
 * It includes methods for generating secret keys, validating codes,
 * generating recovery codes, and generating OTP Auth URLs.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class TwoFactorAuthService {

    private val gAuth = GoogleAuthenticator()
    private val random = SecureRandom()

    /**
     * Generates a new secret key for two-factor authentication.
     *
     * @return A string representing the generated secret key.
     */
    fun generateSecretKey(): String {
        return gAuth.createCredentials().key
    }

    /**
     * Validates a two-factor authentication code against a given secret key.
     *
     * @param secret The secret key used for validation.
     * @param code The two-factor authentication code to validate.
     *
     * @return True if the code is valid, false otherwise.
     */
    fun validateCode(secret: String, code: Int): Boolean {
        return gAuth.authorize(secret, code)
    }

    /**
     * Generates a time-based one-time password (TOTP) using the given secret key.
     *
     * @param secret The secret key used to generate the TOTP.
     */
    fun getTotpPassword(secret: String): Int {
        return gAuth.getTotpPassword(secret)
    }

    /**
     * Generates a recovery code of the specified length.
     *
     * @param length The length of the recovery code to generate.
     *
     * @return A string representing the generated recovery code.
     */
    fun generateRecoveryCode(length: Int = 10): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Generates an OTP Auth URL for the given username and secret key.
     *
     * @param username The username associated with the OTP Auth URL.
     * @param secret The secret key used for generating the OTP Auth URL.
     *
     * @return A string representing the OTP Auth URL.
     */
    fun getOtpAuthUrl(username: String, secret: String): String {
        return "otpauth://totp/$username?secret=$secret&issuer="
    }
}
