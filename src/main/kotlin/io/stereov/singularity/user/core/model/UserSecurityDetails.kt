package io.stereov.singularity.user.core.model

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.database.hash.model.SecureHash
import io.stereov.singularity.global.util.Random
import java.time.Instant

data class UserSecurityDetails(
    val twoFactor: TwoFactorDetails,
    val password: PasswordDetails = PasswordDetails(),
    val mail: MailVerificationDetails = MailVerificationDetails(),
) {

    constructor(mailEnabled: Boolean, mailTwoFactorCodeExpiresIn: Long, mailVerified: Boolean = false): this(
        twoFactor = TwoFactorDetails(
            totp = TwoFactorDetails.TotpDetails(),
            mail = TwoFactorDetails.MailTwoFactorDetails(
                enabled = mailEnabled,
                expiresAt = Instant.now().plusSeconds(mailTwoFactorCodeExpiresIn)
            )
        ),
        mail = MailVerificationDetails(mailVerified)
    )

    data class TwoFactorDetails(
        var preferred: TwoFactorMethod = TwoFactorMethod.MAIL,
        val totp: TotpDetails = TotpDetails(),
        val mail: MailTwoFactorDetails,
    ) {

        private fun getAvailableMethods(): List<TwoFactorMethod> {
            val methods = mutableListOf<TwoFactorMethod>()

            if (totp.enabled) methods.add(TwoFactorMethod.TOTP)
            if (mail.enabled) methods.add(TwoFactorMethod.MAIL)

            return methods
        }

        val methods: List<TwoFactorMethod>
            get() = getAvailableMethods()

        val enabled: Boolean
            get() = methods.isNotEmpty()

        data class TotpDetails(
            var enabled: Boolean = false,
            var secret: String? = null,
            var recoveryCodes: MutableList<SecureHash> = mutableListOf()
        )

        data class MailTwoFactorDetails(
            var enabled: Boolean,
            var code: String = Random.generateInt(6),
            var expiresAt: Instant,
        )
    }

    data class PasswordDetails(
        var resetSecret: String = Random.generateString(20)
    )

    data class MailVerificationDetails(
        var verified: Boolean = false,
        var verificationSecret: String = Random.generateString(20),
    )
}
