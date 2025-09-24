package io.stereov.singularity.user.core.model

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.database.hash.model.SecureHash
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
