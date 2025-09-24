package io.stereov.singularity.user.core.model.identity

import io.stereov.singularity.database.hash.model.SecureHash

data class UserIdentity(
    var password: SecureHash?,
    val principalId: String?,
) {

    companion object {

        fun ofPassword(password: SecureHash): UserIdentity {
            return UserIdentity(password, null)
        }

        fun ofProvider(principalId: String): UserIdentity {
            return UserIdentity(null, principalId)
        }
    }
}
