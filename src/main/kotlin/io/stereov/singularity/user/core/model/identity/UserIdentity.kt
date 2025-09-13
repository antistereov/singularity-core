package io.stereov.singularity.user.core.model.identity

import io.stereov.singularity.database.hash.model.SecureHash

data class UserIdentity(
    var password: SecureHash?,
    val principalId: String?,
    val isPrimary: Boolean = false,
) {

    companion object {

        fun ofPassword(password: SecureHash, isPrimary: Boolean): UserIdentity {
            return UserIdentity(password, null, isPrimary)
        }

        fun ofProvider(principalId: String, isPrimary: Boolean): UserIdentity {
            return UserIdentity(null, principalId, isPrimary)
        }
    }
}
