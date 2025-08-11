package io.stereov.singularity.content.tag.dto

import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.tag.model.TagTranslation

data class CreateTagMultiLangRequest(
    val key: String,
    val translations: MutableMap<Language, TagTranslation>
)
