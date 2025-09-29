package io.stereov.singularity.content.tag.dto

import java.util.*

data class UpdateTagRequest(
    val name: String?,
    val description: String?,
    val locale: Locale?
)
