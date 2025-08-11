package io.stereov.singularity.content.tag.dto

import io.stereov.singularity.content.translate.model.Language

data class UpdateTagRequest(
    val key: String?,
    val translations: Map<Language, TagTranslationUpdateRequest>
) {

    data class TagTranslationUpdateRequest(
        val name: String?,
        val description: String?
    )
}
