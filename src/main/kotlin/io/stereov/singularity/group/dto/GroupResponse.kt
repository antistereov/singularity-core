package io.stereov.singularity.group.dto

import io.stereov.singularity.translate.model.Language

data class GroupResponse (
    val key: String,
    val lang: Language,
    val name: String,
    val description: String
)
