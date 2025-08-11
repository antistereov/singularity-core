package io.stereov.singularity.user.group.dto

import io.stereov.singularity.content.translate.model.Language

data class GroupResponse (
    val key: String,
    val lang: Language,
    val name: String,
    val description: String
)
