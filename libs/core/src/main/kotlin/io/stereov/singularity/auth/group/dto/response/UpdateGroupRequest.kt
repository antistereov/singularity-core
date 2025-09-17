package io.stereov.singularity.auth.group.dto.response

import io.stereov.singularity.auth.group.model.GroupTranslation
import java.util.*

data class UpdateGroupRequest(
    val translations: MutableMap<Locale, GroupTranslation>
)
