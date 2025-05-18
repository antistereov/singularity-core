package io.stereov.singularity.content.common.tag.dto

data class CreateTagRequest(
    val name: String,
    val description: String? = null,
)
