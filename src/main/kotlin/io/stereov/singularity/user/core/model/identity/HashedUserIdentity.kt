package io.stereov.singularity.user.core.model.identity

import io.stereov.singularity.database.hash.model.SearchableHash
import io.stereov.singularity.database.hash.model.Hash

data class HashedUserIdentity(
    val password: Hash?,
    val principalId: SearchableHash?,
)
