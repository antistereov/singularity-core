package io.stereov.singularity.content.tag.dto

import java.util.*

data class UpdateTagRequest(
    val key: String?,
    val translations: Map<Locale, TagTranslationUpdateRequest>
) {

    data class TagTranslationUpdateRequest(
        val name: String?,
        val description: String?
    )
}
