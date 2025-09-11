package io.stereov.singularity.auth.twofactor.service

import com.warrenstrange.googleauth.GoogleAuthenticator
import org.springframework.stereotype.Service

@Service
class TotpService(
    private val gAuth: GoogleAuthenticator,
) {

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
    fun codeIsValid(secret: String, code: Int): Boolean {
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
