package io.stereov.singularity.group.dto

import io.stereov.singularity.global.language.model.Language

data class GroupResponse (
    val key: String,
    val lang: Language,
    val name: String,
    val description: String
)
