package io.stereov.singularity.content.tag.dto

import java.util.Locale

data class TagResponse(
    val key: String,
    val locale: Locale,
    val name: String,
    val description: String
)
