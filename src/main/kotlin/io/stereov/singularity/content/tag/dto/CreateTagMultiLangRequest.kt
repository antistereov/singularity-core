package io.stereov.singularity.content.tag.dto

import io.stereov.singularity.content.tag.model.TagTranslation
import java.util.*

data class CreateTagMultiLangRequest(
    val key: String,
    val translations: MutableMap<Locale, TagTranslation>,
)
