package io.stereov.singularity.content.common.tag.dto

data class KeyContainsResponse(
    val tags: List<TagResponse>,
    val size: Int
)
