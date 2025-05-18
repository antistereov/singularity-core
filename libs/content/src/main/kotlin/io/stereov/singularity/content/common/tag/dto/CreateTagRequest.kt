package io.stereov.singularity.content.common.tag.dto

data class CreateTagRequest(
    val key: String,
    val description: String? = null,
)
