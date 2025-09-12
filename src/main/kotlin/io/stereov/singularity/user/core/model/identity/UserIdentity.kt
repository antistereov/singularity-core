package io.stereov.singularity.user.core.model.identity

import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.database.hash.model.SecureHash

data class UserIdentity(
    val provider: String,
    var password: SecureHash?,
    val principalId: String?,
    val isPrimary: Boolean = false,
) {

    companion object {

        fun ofPassword(password: SecureHash, isPrimary: Boolean): UserIdentity {
            return UserIdentity(IdentityProvider.PASSWORD, password, null, isPrimary)
        }

        fun ofProvider(provider: String, principalId: String, isPrimary: Boolean): UserIdentity {
            return UserIdentity(provider, null, principalId, isPrimary)
        }
    }
}

