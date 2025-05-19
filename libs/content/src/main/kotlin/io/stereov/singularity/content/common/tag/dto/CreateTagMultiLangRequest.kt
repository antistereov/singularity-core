package io.stereov.singularity.content.common.tag.dto

import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.content.common.tag.model.TagTranslation

data class CreateTagMultiLangRequest(
    val key: String,
    val translations: MutableMap<Language, TagTranslation>
)
