package io.stereov.singularity.content.tag.dto

data class KeyContainsResponse(
    val tags: List<TagResponse>,
    val size: Int
)
