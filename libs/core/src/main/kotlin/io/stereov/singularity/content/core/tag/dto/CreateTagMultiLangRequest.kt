package io.stereov.singularity.content.core.tag.dto

import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.core.tag.model.TagTranslation

data class CreateTagMultiLangRequest(
    val key: String,
    val translations: MutableMap<Language, TagTranslation>
)
