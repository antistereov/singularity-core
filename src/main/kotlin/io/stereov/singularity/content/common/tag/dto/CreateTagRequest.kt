package io.stereov.singularity.content.common.tag.dto

import io.stereov.singularity.translate.model.Language

data class CreateTagRequest(
    val key: String,
    val lang: Language = Language.EN,
    val name: String,
    val description: String? = null,
)
