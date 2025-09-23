package io.stereov.singularity.user.core.model.identity

import io.stereov.singularity.database.hash.model.SearchableHash
import io.stereov.singularity.database.hash.model.SecureHash

data class HashedUserIdentity(
    val password: SecureHash?,
    val principalId: SearchableHash?,
)
