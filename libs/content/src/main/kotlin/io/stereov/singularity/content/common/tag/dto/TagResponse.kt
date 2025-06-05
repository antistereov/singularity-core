package io.stereov.singularity.content.common.tag.dto

import io.stereov.singularity.global.language.model.Language

data class TagResponse(
    val key: String,
    val lang: Language,
    val name: String,
    val description: String
)
