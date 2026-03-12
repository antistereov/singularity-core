package io.stereov.singularity.principal.group.dto.response

import io.stereov.singularity.database.core.model.DocumentKey
import java.util.*

data class GroupResponse (
    val key: DocumentKey,
    val locale: Locale,
    val name: String,
    val description: String
)
