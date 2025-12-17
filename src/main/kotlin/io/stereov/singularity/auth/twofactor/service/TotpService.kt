package io.stereov.singularity.auth.twofactor.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.twofactor.exception.TotpException
import io.stereov.singularity.global.properties.AppProperties
import org.springframework.stereotype.Service

/**
 * A service for handling TOTP (Time-based One-Time Password) related operations such as
 * generating secrets, validating codes, and generating passwords.
 *
 * This service uses the [GoogleAuthenticator] library to manage TOTP-based authentication.
 */
@Service
class TotpService(
    private val gAuth: GoogleAuthenticator,
    private val appProperties: AppProperties
) {

    /**
     * Generates a new TOTP secret key.
     *
     * @return A [Result] containing the generated secret key as a string if successful,
     * or an instance of [TotpException.GenerateSecret] if an error occurs.
     */
    fun generateSecretKey(): Result<String, TotpException.GenerateSecret> {
        return runCatching { gAuth.createCredentials().key }
            .mapError { ex -> TotpException.GenerateSecret("Failed to generate TOTP secret: ${ex.message}", ex) }
    }


    /**
     * Validates whether the provided TOTP code is valid for the given secret key.
     *
     * @param secret The secret key corresponding to the TOTP code.
     * @param code The TOTP code to validate.
     * @return A [Result] containing `true` if the code is valid, `false` otherwise,
     * or an instance of [TotpException.Validation] in case of an error.
     */
    fun codeIsValid(secret: String, code: Int): Result<Boolean, TotpException.Validation> {
        return runCatching { gAuth.authorize(secret, code) }
            .mapError { ex -> TotpException.Validation("Failed to validate TOTP code: ${ex.message}", ex) }
    }

    /**
     * Generates a TOTP password based on the provided secret key.
     *
     * @param secret The secret key used to generate the TOTP password.
     * @return A [Result] containing the generated TOTP password as an integer if successful,
     * or an instance of [TotpException.GeneratePassword] if an error occurs during the generation process.
     */
    fun getTotpPassword(secret: String): Result<Int, TotpException.GeneratePassword> {
        return runCatching { gAuth.getTotpPassword(secret) }
            .mapError { ex -> TotpException.GeneratePassword("Failed to generate TOTP: ${ex.message}", ex) }
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
        return "otpauth://totp/$username?secret=$secret&issuer=${appProperties.name}"
    }
}
