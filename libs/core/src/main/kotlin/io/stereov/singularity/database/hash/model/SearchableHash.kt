package io.stereov.singularity.database.hash.model

import java.util.*

data class SearchableHash(
    val data: String,
    val secretId: UUID,
)
