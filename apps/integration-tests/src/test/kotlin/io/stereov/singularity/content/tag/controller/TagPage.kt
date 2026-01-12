package io.stereov.singularity.content.tag.controller

import io.stereov.singularity.content.tag.dto.TagResponse
import org.springframework.data.web.PagedModel

data class TagPage(
    val content: List<TagResponse> = emptyList(),
    val page: PagedModel.PageMetadata
)