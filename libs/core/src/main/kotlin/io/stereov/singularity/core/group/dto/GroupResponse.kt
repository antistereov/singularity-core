package io.stereov.singularity.core.group.dto

import io.stereov.singularity.core.global.language.model.Language

data class GroupResponse (
    val key: String,
    val lang: Language,
    val name: String,
    val description: String
)
