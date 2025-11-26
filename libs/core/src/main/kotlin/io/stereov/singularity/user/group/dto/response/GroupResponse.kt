package io.stereov.singularity.user.group.dto.response

import java.util.*

data class GroupResponse (
    val key: String,
    val locale: Locale,
    val name: String,
    val description: String
)
