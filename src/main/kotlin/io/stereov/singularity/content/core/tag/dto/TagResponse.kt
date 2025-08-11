package io.stereov.singularity.content.core.tag.dto

import io.stereov.singularity.content.translate.model.Language

data class TagResponse(
    val key: String,
    val lang: Language,
    val name: String,
    val description: String
)
