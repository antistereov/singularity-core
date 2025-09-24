package io.stereov.singularity.user.core.model.identity

import io.stereov.singularity.database.hash.model.Hash

data class UserIdentity(
    var password: Hash?,
    val principalId: String?,
) {

    companion object {

        fun ofPassword(password: Hash): UserIdentity {
            return UserIdentity(password, null)
        }

        fun ofProvider(principalId: String): UserIdentity {
            return UserIdentity(null, principalId)
        }
    }
}
