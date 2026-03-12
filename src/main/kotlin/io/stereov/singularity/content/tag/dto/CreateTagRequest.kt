package io.stereov.singularity.content.tag.dto

import io.stereov.singularity.database.core.model.DocumentKey
import java.util.*

data class CreateTagRequest(
    val key: DocumentKey,
    val locale: Locale?,
    val name: String,
    val description: String? = null,
)
