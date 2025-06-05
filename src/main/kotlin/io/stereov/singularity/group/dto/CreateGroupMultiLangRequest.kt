package io.stereov.singularity.group.dto

import io.stereov.singularity.global.language.model.Language
import io.stereov.singularity.group.model.GroupTranslation

data class CreateGroupMultiLangRequest(
    val key: String,
    val translations: MutableMap<Language, GroupTranslation>
)
