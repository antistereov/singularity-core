package io.stereov.singularity.content.common.tag.dto

import io.stereov.singularity.content.common.tag.model.TagDocument

data class NameContainsResponse(
    val tags: List<TagDocument>,
    val size: Int
)
