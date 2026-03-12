package io.stereov.singularity.content.tag.dto

import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.database.core.model.DocumentKey
import java.util.*

data class CreateTagMultiLangRequest(
    val key: DocumentKey,
    val translations: MutableMap<Locale, TagTranslation>,
)
