package io.stereov.singularity.principal.core.model

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.database.hash.model.Hash
import io.stereov.singularity.global.util.Random
import java.time.Instant

data class UserSecurityDetails(
    val twoFactor: TwoFactorDetails,
    val password: PasswordDetails = PasswordDetails(),
    val email: MailVerificationDetails = MailVerificationDetails(),
) {

    constructor(email2faEnabled: Boolean, mailTwoFactorCodeExpiresIn: Long, mailVerified: Boolean = false): this(
        twoFactor = TwoFactorDetails(
            totp = TwoFactorDetails.TotpDetails(),
            email = TwoFactorDetails.MailTwoFactorDetails(
                enabled = email2faEnabled,
                expiresAt = Instant.now().plusSeconds(mailTwoFactorCodeExpiresIn)
            )
        ),
        email = MailVerificationDetails(mailVerified)
    )

    /**
     * Represents the two-factor authentication details for a user.
     *
     * This class contains the configuration and state of two-factor authentication,
     * including preferred methods, available methods, and status of each method. It also
     * provides information such as whether two-factor authentication is enabled and the
     * methods that are currently available for the user.
     *
     * @property preferred The user's preferred method of two-factor authentication. Defaults to [TwoFactorMethod.EMAIL].
     * @property totp The configuration and state details for TOTP-based two-factor authentication.
     * @property email The configuration and state details for email-based two-factor authentication.
     */
    data class TwoFactorDetails(
        var preferred: TwoFactorMethod = TwoFactorMethod.EMAIL,
        val totp: TotpDetails = TotpDetails(),
        val email: MailTwoFactorDetails,
    ) {

        private fun getAvailableMethods(): List<TwoFactorMethod> {
            val methods = mutableListOf<TwoFactorMethod>()

            if (totp.enabled) methods.add(TwoFactorMethod.TOTP)
            if (email.enabled) methods.add(TwoFactorMethod.EMAIL)

            return methods
        }

        /**
         * Retrieves the list of available two-factor authentication methods.
         *
         * This property dynamically constructs the list of methods based on the enabled status
         * of two-factor authentication options such as TOTP and email. If a method is enabled,
         * it is included in the returned list.
         *
         * @return A list containing the available [TwoFactorMethod] options.
         */
        val methods: List<TwoFactorMethod>
            get() = getAvailableMethods()

        /**
         * Indicates whether any two-factor authentication methods are available.
         *
         * This property dynamically evaluates the presence of available two-factor methods
         * by checking if the `methods` list is not empty.
         *
         * @return `true` if methods are available, otherwise `false`.
         */
        val enabled: Boolean
            get() = methods.isNotEmpty()

        /**
         * Represents the details of Time-based One-Time Password (TOTP) authentication.
         *
         * This class contains the configuration and recovery state for TOTP-based
         * two-factor authentication, including whether it is enabled, the secret key
         * used for generating codes, and a list of recovery codes.
         *
         * @property enabled Indicates whether TOTP-based authentication is currently enabled.
         * @property secret The shared secret key used for generating TOTP codes. This value may be null.
         * @property recoveryCodes A list of recovery codes associated with the TOTP setup. These codes can be used as a fallback
         * to authenticate if the TOTP device is unavailable.
         */
        data class TotpDetails(
            var enabled: Boolean = false,
            var secret: String? = null,
            var recoveryCodes: MutableList<Hash> = mutableListOf()
        )

        /**
         * Represents the details used for email-based two-factor authentication (2FA).
         *
         * This class contains critical information required for email-based 2FA, such as whether
         * the feature is enabled, the generated authentication code, and the expiration timestamp
         * for the code.
         *
         * @property enabled Indicates whether email-based two-factor authentication is currently enabled.
         * @property code A randomly generated numeric code, represented as a string, used for authentication.
         * This is typically sent to the user's email and should meet a specific length requirement.
         * @property expiresAt The timestamp indicating when the authentication code expires. After this timestamp,
         * the code will no longer be valid for authentication purposes.
         */
        data class MailTwoFactorDetails(
            var enabled: Boolean,
            var code: String = Random.generateInt(6).getOrThrow(),
            var expiresAt: Instant,
        )
    }

    /**
     * Represents details related to password management for a user.
     *
     * This class is a part of the security configuration and plays a key role
     * in handling sensitive data used for password reset and recovery functionalities.
     *
     * @property resetSecret A randomly generated secret string used for password reset purposes.
     */
    data class PasswordDetails(
        var resetSecret: String = Random.generateString(20).getOrThrow()
    )

    /**
     * Represents the details related to mail verification for a user.
     *
     * This data class is used to track whether a user's email has been verified,
     * and to store a secret string used for the verification process.
     *
     * @property verified Indicates if the email has been successfully verified.
     * Defaults to `false`, meaning the email is unverified by default.
     *
     * @property verificationSecret The secret string used in the mail verification process.
     * This string is randomly generated with a default length of 20 characters.
     */
    data class MailVerificationDetails(
        var verified: Boolean = false,
        var verificationSecret: String = Random.generateString(20).getOrThrow(),
    )
}
