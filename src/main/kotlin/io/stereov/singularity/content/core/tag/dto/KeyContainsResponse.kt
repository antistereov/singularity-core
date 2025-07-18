package io.stereov.singularity.content.core.tag.dto

data class KeyContainsResponse(
    val tags: List<TagResponse>,
    val size: Int
)
