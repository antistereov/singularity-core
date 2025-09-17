package io.stereov.singularity.content.tag.dto

data class CreateTagRequest(
    val key: String,
    val languageTag: String,
    val name: String,
    val description: String? = null,
)
