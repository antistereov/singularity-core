package io.stereov.singularity.auth.group.dto.response

import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.content.translate.model.Language

data class UpdateGroupRequest(
    val translations: MutableMap<Language, GroupTranslation>
)
