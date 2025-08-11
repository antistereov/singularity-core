package io.stereov.singularity.auth.twofactor.service

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.core.exception.model.TwoFactorAuthDisabledException
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorCodeException
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.user.core.model.UserDocument
import org.springframework.stereotype.Service

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
class TwoFactorAuthService(
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

    /**
     * Validates the two-factor code for the given user. It throws an exception if the code is invalid.
     *
     * @param user The user to validate the code for.
     * @param code The two-factor code to validate.
     *
     * @throws InvalidDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws InvalidTwoFactorCodeException If the two-factor code is invalid.
     */
    suspend fun validateTwoFactorCode(user: UserDocument, code: Int): UserDocument {
        val secret = user.sensitive.security.twoFactor.secret
            ?: throw TwoFactorAuthDisabledException()

        if (!validateCode(secret, code)) {
            throw InvalidTwoFactorCodeException()
        }

        return user
    }
}
