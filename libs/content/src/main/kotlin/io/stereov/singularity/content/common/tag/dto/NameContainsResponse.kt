package io.stereov.singularity.content.common.tag.dto

data class NameContainsResponse(
    val tags: List<TagResponse>,
    val size: Int
)
