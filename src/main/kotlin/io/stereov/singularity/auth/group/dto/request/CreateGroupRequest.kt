package io.stereov.singularity.auth.group.dto.request

import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.content.translate.model.Language

data class CreateGroupRequest(
    val key: String,
    val translations: MutableMap<Language, GroupTranslation>
)
