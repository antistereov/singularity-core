package io.stereov.singularity.auth.group.dto.request

import io.stereov.singularity.auth.group.model.GroupTranslation
import java.util.*

data class CreateGroupRequest(
    val key: String,
    val translations: MutableMap<Locale, GroupTranslation>,
    val primaryLocale: Locale?,
)
