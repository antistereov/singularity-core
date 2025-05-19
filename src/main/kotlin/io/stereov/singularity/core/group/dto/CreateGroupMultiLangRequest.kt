package io.stereov.singularity.core.group.dto

import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.group.model.GroupTranslation

data class CreateGroupMultiLangRequest(
    val key: String,
    val translations: MutableMap<Language, GroupTranslation>
)
