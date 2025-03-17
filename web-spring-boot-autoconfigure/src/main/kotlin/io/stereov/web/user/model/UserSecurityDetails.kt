package io.stereov.web.user.model

import java.util.UUID

data class UserSecurityDetails(
    val twoFactor: TwoFactorDetails = TwoFactorDetails(),
    val mail: MailVerificationDetails = MailVerificationDetails(),
) {

    data class TwoFactorDetails(
        var enabled: Boolean = false,
        var secret: String? = null,
        var recoveryCode: String? = null
    )

    data class MailVerificationDetails(
        var verificationUuid: String = UUID.randomUUID().toString(),
        var verified: Boolean = false,
    )
}
