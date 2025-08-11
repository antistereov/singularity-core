package io.stereov.singularity.user.group.dto

import io.stereov.singularity.user.group.model.GroupTranslation
import io.stereov.singularity.content.translate.model.Language

data class CreateGroupRequest(
    val key: String,
    val translations: MutableMap<Language, GroupTranslation>
)
