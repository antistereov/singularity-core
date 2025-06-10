package io.stereov.singularity.group.dto

import io.stereov.singularity.group.model.GroupTranslation
import io.stereov.singularity.translate.model.Language

data class UpdateGroupRequest(
    val key: String,
    val translations: MutableMap<Language, GroupTranslation>
)
