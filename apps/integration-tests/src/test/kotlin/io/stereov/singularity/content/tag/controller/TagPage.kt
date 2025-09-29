package io.stereov.singularity.content.tag.controller

import io.stereov.singularity.content.tag.dto.TagResponse

data class TagPage(
    val content: List<TagResponse> = emptyList(),
    val pageNumber: Int,
    val pageSize: Int,
    val numberOfElements: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)