package io.stereov.singularity.content.common.tag.dto

import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.content.common.tag.model.TagTranslation

data class CreateTagMultiLangRequest(
    val key: String,
    val translations: MutableMap<Language, TagTranslation>
)
