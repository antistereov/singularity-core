package io.stereov.web.user.model

import java.util.UUID

/**
 * # UserSecurityDetails
 *
 * This class represents the security details of a user, including two-factor authentication and email verification.
 *
 * @property twoFactor The two-factor authentication details of the user.
 * @property mail The email verification details of the user.
 */
data class UserSecurityDetails(
    val twoFactor: TwoFactorDetails = TwoFactorDetails(),
    val mail: MailVerificationDetails = MailVerificationDetails(),
) {

    /**
     * ## TwoFactorDetails
     *
     * This class represents the two-factor authentication details of a user.
     *
     * @property enabled Indicates whether two-factor authentication is enabled.
     * @property secret The secret key for the user.
     * @property recoveryCode The recovery code for the user.
     */
    data class TwoFactorDetails(
        var enabled: Boolean = false,
        var secret: String? = null,
        var recoveryCode: String? = null
    )

    /**
     * ## MailVerificationDetails
     *
     * This class represents the email verification details of a user.
     *
     * @property verificationUuid The UUID for email verification.
     * @property verified Indicates whether the email is verified.
     */
    data class MailVerificationDetails(
        var verificationUuid: String = UUID.randomUUID().toString(),
        var verified: Boolean = false,
    )
}
