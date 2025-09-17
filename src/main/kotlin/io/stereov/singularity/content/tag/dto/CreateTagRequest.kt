package io.stereov.singularity.content.tag.dto

import java.util.*

data class CreateTagRequest(
    val key: String,
    val locale: Locale?,
    val name: String,
    val description: String? = null,
)
